import models.Bind;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller {
    String name;
    Class modelClass;
    Object model;
    String[] LATA;
    Map<String, Object> bindings = new HashMap<>();


    ScriptEngineManager sem;

    public Controller(String modelName) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        name = modelName;
        try{
            modelClass = Class.forName("models."+modelName);
        }catch(ClassNotFoundException e){
            System.out.println("Model not found");
        }
        model = modelClass.getDeclaredConstructor().newInstance();
        sem = new ScriptEngineManager();
    }

    public Controller readDataFromFile(String filename) throws FileNotFoundException{
        var map = parseFileToMap(filename);
        LATA =  map.get("LATA").stream().map(Double::intValue).map(Object::toString).toArray(String[]::new);
        fillModel(map);
        return this;
    }

   public Controller runModel() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method run = modelClass.getMethod("run");
        run.invoke(model, null);
        return this;
    }

    public Controller runScriptFromFile(String filename) throws IOException, ScriptException {
        String extension = filename.split("\\.")[filename.split("\\.").length - 1];
        ScriptEngine engine = sem.getEngineByExtension(extension);
        String script = Files.readString(Paths.get(filename));

        return runScriptFromString(script, engine);
    }

    public Controller runScriptFromString(String script, ScriptEngine engine) throws ScriptException {
        fillScriptEngineWithVars(engine);
        engine.eval(script);
        fillVarsFromScripEngine(engine);

        return this;
    }


    public String getLATA(){
        return Arrays.stream(LATA).map(Object::toString).collect(Collectors.joining("\t"));
    }

    private HashMap<String, ArrayList<Double>> parseFileToMap(String filename) throws FileNotFoundException {
        HashMap<String, ArrayList<Double>> data = new HashMap<>();

        try{
            Stream<String> lines = Files.lines(Path.of(filename));

            return (HashMap<String, ArrayList<Double>>) lines.map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toMap(line -> line.split("\\s+")[0],
                            line ->
                                Arrays.stream(line.split("\\s+"))
                                        .skip(1)
                                        .map(Double::parseDouble).collect(Collectors.toCollection(ArrayList::new))
                            ));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillModel(HashMap<String, ArrayList<Double>> map){
        final int size = map.get("LATA").size();
        getBindedFields().forEach(
                field -> {
                    field.setAccessible(true);
                    try {
                        if (field.getName().equals("LL")) {
                            field.setInt(model, size);
                        }
                        else field.set(model, makeArray(map.get(field.getName()), size));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

        );
    }

    private Stream<Field> getBindedFields() {
        return Arrays.stream(modelClass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Bind.class));
    }

    private void fillVarsFromScripEngine(ScriptEngine engine) throws ScriptException {
        Map<String, Object> sbinds = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        getBindedFields().forEach(field -> {
            if(sbinds.containsKey(field.getName())){
                field.setAccessible(true);
                try {
                    field.set(model, sbinds.get(field.getName()));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                sbinds.remove(field.getName());
            }
        });

        sbinds.keySet().stream()
                .filter(key -> !(key.length()==1 && Character.isLowerCase(key.charAt(0))))
                .forEach(key -> bindings.put(key, sbinds.get(key)));

    }


    private void fillScriptEngineWithVars(ScriptEngine engine) {
        getBindedFields().forEach(field -> {
            field.setAccessible(true);
            try {
                engine.put(field.getName(), field.get(model));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        bindings.keySet().forEach(key -> engine.put(key, bindings.get(key)));
    }
    private double[] makeArray(ArrayList<Double> array, int size){
        double[] result = new double[size];
        int i = 0;
        if(array == null) return result;
        while(i<array.size()){
            result[i] = array.get(i);
            i++;
        }
        while(i<result.length){
            result[i] = result[i-1];
            i++;
        }
        return result;
    }


    public String getResultsAsTsv(){
        StringBuilder result = new StringBuilder();
        result.append("LATA\t").append(String.join("\t", getLATA()));
        result.append("\n");

        try{
            getBindedFields().filter(field -> field.getType().isArray())
                    .forEachOrdered(field -> {
                        field.setAccessible(true);
                        try {
                            double[] val = (double[]) field.get(model);
                            result.append(field.getName()).append("\t");
                            result.append(Arrays.stream(val).mapToObj(String::valueOf).collect(Collectors.joining("\t")));
                            result.append("\n");
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }catch(Exception e){}

        if(!bindings.isEmpty()){
            bindings.keySet().stream().forEach(key-> {
                Object value = bindings.get(key);
                if(value==null) return;
                result.append(key).append("\t");
                if(value.getClass().isArray()){
                    result.append(Arrays.stream((double[]) value).mapToObj(String::valueOf).collect(Collectors.joining("\t")));
                }else {
                    result.append(value.toString());
                }
                result.append("\n");
            });
        }

        return result.toString();
    }

    Consumer<Object> printTab = obj -> System.out.print(obj+"\t");
    DoubleConsumer printTabDouble = obj -> System.out.print(obj+"\t");

    public void printFields() throws IllegalAccessException {
        System.out.print("LATA\t");
        Arrays.stream(LATA).forEach(printTab);
        System.out.println();

        getBindedFields().forEach(field -> {
            field.setAccessible(true);
            if(!field.getType().isArray()) return;

            try {
                double[] array = (double[]) field.get(model);
                System.out.print(field.getName()+"\t");
                Arrays.stream(array).forEach(printTabDouble);
                System.out.println();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        if(!bindings.isEmpty()){
            bindings.keySet().stream().forEach(key-> {
                Object value = bindings.get(key);
                if(value==null) return;
                System.out.print(key+"\t");
                if(value.getClass().isArray()){
                    Arrays.stream((double[]) value).forEach(printTabDouble);
                    System.out.println();
                }else {
                    System.out.print(value+"\t");
                }
            });
        }
    }

    public static void main(String[] args) throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, ScriptException {
        Controller controller = new Controller("Model1").readDataFromFile("data/data2.txt");
        controller.runModel();
        String s = controller.getResultsAsTsv();
        System.out.println(Arrays.deepToString(s.split("\t")));
//        controller.runScriptFromFile("scripts/script1.groovy");
//        controller.printFields();
    }
}
