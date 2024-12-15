import models.Bind;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller {
    String name;
    Class modelClass;
    Object model;
    Object[] LATA;

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
        LATA = map.get("LATA").stream().map(Double::intValue).toArray();
        fillModel(map);
        return this;
    }

   public Controller runModel() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method run = modelClass.getMethod("run");
        System.out.println("invoking method");
        run.invoke(model, null);
        return this;
    }

    public Controller runScriptFromFile(String filename){
        for(var engine: sem.getEngineFactories()){
            System.out.println(engine.getEngineName());
        }

        return this;
    }

    public Controller runScriptFromString(String script){
        return this;
    }

    public String getResultsAsTsv(){
        return "";
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
        Arrays.stream(modelClass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Bind.class)).forEach(
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

    public void printFields() throws IllegalAccessException {
        Arrays.stream(LATA).forEach(lat -> System.out.print(lat + " "));
        System.out.println();
        for(Field field : modelClass.getDeclaredFields()){
            field.setAccessible(true);
            if(field.getType().getName().equals("int")){
                System.out.println(field.getName() + " : " + field.getInt(model));
            }else{
                System.out.println(field.getName() + " : " + Arrays.deepToString(new Object[]{field.get(model)}));
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        Controller controller = new Controller("Model1").readDataFromFile("data/data2.txt");
        controller.runModel();
        controller.printFields();
        controller.runScriptFromFile("data/data2.txt");
    }


}
