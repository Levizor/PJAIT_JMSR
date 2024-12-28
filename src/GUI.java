import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Consumer;

public class GUI extends JFrame{
    private JTable table;
    private DefaultTableModel tableModel;

    private JList<String> modelsList;
    private DefaultListModel<String> modelsListModel;
    private File modelsDirectory;
    private JScrollPane tableScrollPane;
    private JList<String> dataList;
    private DefaultListModel<String> dataListModel;
    private File dataDirectory;
    private boolean dataLoaded = false;

    private Controller controller;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }

    public GUI() {
        // Create main window
        super("Modelling&Scripting");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Left Panel: List of files
        JPanel leftPanel = new JPanel(new BorderLayout(2, 2));
        modelsListModel = new DefaultListModel<>();
        modelsList = new JList<>(modelsListModel);
        dataListModel = new DefaultListModel<>();
        dataList = new JList<>(dataListModel);
        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        centerPanel.add(new JScrollPane(modelsList));
        centerPanel.add(new JScrollPane(dataList));
        leftPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());

        JSplitPane pane = new JSplitPane();

        pane.setLeftComponent(leftPanel);
        pane.setRightComponent(rightPanel);
        // Buttons for loading files
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton loadModelsButton = new JButton("Load Models");
        JButton loadDataButton = new JButton("Load data");

        buttonPanel.add(loadModelsButton);
        buttonPanel.add(loadDataButton);

        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Center Panel: JTable
        String[] cols = new String[]{"Model", "Name"};
        table = new JTable(tableModel);
        tableScrollPane = new JScrollPane(table);

        // Bottom Panel: Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton runModelButton = new JButton("Run Model");
        JButton scriptFromFileButton = new JButton("Run script from file");
        JButton createScriptButton = new JButton("Create and Run Ad-hoc Script");

        bottomPanel.add(runModelButton);
        bottomPanel.add(scriptFromFileButton);
        bottomPanel.add(createScriptButton);

        rightPanel.add(bottomPanel, BorderLayout.SOUTH);
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Add components to this
        add(pane, BorderLayout.CENTER);

        // Action Listeners
        loadModelsButton.addActionListener(e -> loadSomething(modelsListModel, this::setModelsDirectory, "Choose model to run", "java"));
        loadDataButton.addActionListener(e -> loadSomething(dataListModel, this::setDataDirectory, "Choose data to run", ""));
        createScriptButton.addActionListener(e -> openScriptEditor());
        scriptFromFileButton.addActionListener(e -> runScriptFromFile());
        runModelButton.addActionListener(e -> runModel());

        setVisible(true);
    }

    // Load models and data files from a chosen directory
    private void loadSomething(DefaultListModel<String> lm, Consumer<File> directorysetter, String chooser_name, String data_extension) {
        JFileChooser chooser = new JFileChooser(chooser_name);
        chooser.setCurrentDirectory(Paths.get(".").toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = chooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File directory = chooser.getSelectedFile();
            directorysetter.accept(directory);
            lm.clear();
            File[] files = directory.listFiles((dir, name) -> name.endsWith(data_extension) && !name.equals("Bind.java"));
            if (files != null) {
                for (File file : files) {
                    lm.addElement(file.getName());
                }
            }
        }
    }

    private void setDataDirectory(File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    private void setModelsDirectory(File modelsDirectory) {
        this.modelsDirectory = modelsDirectory;
    }

    private void runScriptFromFile() {
        if(!dataLoaded) {
            JOptionPane.showMessageDialog(this, "No data to run scripts on.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        int returnValue = chooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try{
                controller.runScriptFromFile(selectedFile.getAbsolutePath().toString());
            } catch (IOException e){
                JOptionPane.showMessageDialog(this, "Script not found", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ScriptException e) {
                JOptionPane.showMessageDialog(this, "Script error", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        showResultsFromTSV(controller.getResultsAsTsv());
    }

    private void openScriptEditor() {
        if(!dataLoaded) {
            JOptionPane.showMessageDialog(this, "No data to run scripts on.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFrame editorFrame = new JFrame("Ad-hoc Script Editor");
        editorFrame.setLocationRelativeTo(this);
        editorFrame.setSize(600, 400);
        editorFrame.setLayout(new BorderLayout());

        JTextArea scriptArea = new JTextArea();
        JScrollPane scriptScrollPane = new JScrollPane(scriptArea);
        JButton runButton = new JButton("Run Script");
        JButton cancel = new JButton("Cancel");

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(runButton);
        bottomPanel.add(cancel);

        editorFrame.add(scriptScrollPane, BorderLayout.CENTER);
        editorFrame.add(bottomPanel, BorderLayout.SOUTH);

        runButton.addActionListener(e -> {
            String script = scriptArea.getText();
            try {
                editorFrame.dispose();
                controller.runScriptFromString(script, controller.sem.getEngineByName("groovy"));
                showResultsFromTSV(controller.getResultsAsTsv());
            } catch (ScriptException ex) {
                JOptionPane.showMessageDialog(this, "Error while running the script: %s".formatted(ex.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancel.addActionListener(e -> {
            editorFrame.dispose();
        });

        editorFrame.setVisible(true);
    }

    private void showResultsFromTSV(String tsv) {
        String[] rows = tsv.split("\n");
        tableModel = new DefaultTableModel(rows[0].split("\t"), 0);
        Arrays.stream(rows).skip(1).forEach(row -> {
            String[] cells = row.split("\t");
            tableModel.addRow(cells);
        });
        table.setModel(tableModel);
        tableModel.fireTableDataChanged();
    }

    private void runModel(){
        String selectedModel = modelsList.getSelectedValue();
        String selectedData = dataList.getSelectedValue();

        if (selectedModel == null) {
            JOptionPane.showMessageDialog(this, "No model selected!");
            return;
        } else if (selectedData == null) {
            JOptionPane.showMessageDialog(this, "No data selected!");
            return;
        }
        selectedModel = selectedModel.substring(0, selectedModel.indexOf(".java"));
        selectedData = dataDirectory.getAbsolutePath() + File.separator + selectedData;

        try {
            controller = new Controller(selectedModel);
        } catch
         (InvocationTargetException| InstantiationException| IllegalAccessException| NoSuchMethodException e){
            JOptionPane.showMessageDialog(this, "Error running model: " + selectedModel);
            return;
        }

        try {
            controller.readDataFromFile(selectedData);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, "File not found: " + selectedData);
            return;
        }

        try {
            String results = controller.runModel().getResultsAsTsv();
            showResultsFromTSV(results);
            dataLoaded = true;
        } catch (NoSuchMethodException e) {
            JOptionPane.showMessageDialog(this, "No run method found in the model" + selectedModel);
            return;
        } catch (InvocationTargetException | IllegalAccessException  e) {
            throw new RuntimeException(e);
        }
    }

}
