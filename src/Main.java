import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    JPanel opts;
    JPanel view;

    public Main(String name) {
        super(name);
        opts=new JPanel();
        view=new JPanel();
        opts.add(new JButton("Exit"));
        view.add(new JButton("Exit"));
        add(opts, BorderLayout.WEST);
        add(view, BorderLayout.CENTER);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    public void run(){
        setVisible(true);
    }
    public static void main(String[] args) throws ScriptException {
        new Main("Analyzer").run();
//        ScriptEngine engine=new ScriptEngineManager().getEngineByName("groovy");
//        engine.put("x", 1);
//        engine.eval("x = x+2");
//        int x = (int) engine.get("x");
//        System.out.println(x);
    }
}