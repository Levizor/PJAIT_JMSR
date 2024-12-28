package models;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ModelDif {
    @Bind
    private int LL;
    @Bind
    private double[] growthPrivateConsumption;
    @Bind
    private double[] growthPublicConsumption;
    @Bind
    private double[] growthInvestment;
    @Bind
    private double[] growthExports;
    @Bind
    private double[] growthImports;
    @Bind
    private double[] privateConsumption;
    @Bind
    private double[] publicConsumption;
    @Bind
    private double[] investment;
    @Bind
    private double[] exports;
    @Bind
    private double[] imports;
    @Bind
    private double[] GDP;

    public ModelDif() {}

    public void run() {
        GDP = new double[LL];
        for (int t = 0; t < LL; t++) {
            if (t > 0) {
                privateConsumption[t] = privateConsumption[t - 1] * growthPrivateConsumption[t] + Math.sin(t);
                publicConsumption[t] = publicConsumption[t - 1] * growthPublicConsumption[t] - Math.cos(t);
                investment[t] = investment[t - 1] * growthInvestment[t] * Math.log(t + 1);
                exports[t] = exports[t - 1] * growthExports[t] + Math.pow(t, 2);
                imports[t] = imports[t - 1] * growthImports[t] + Math.sqrt(t);
            }
            GDP[t] = privateConsumption[t] + publicConsumption[t] + investment[t] + exports[t] - imports[t];
        }
    }
}
