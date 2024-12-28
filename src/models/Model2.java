package models;

public class Model2 {
    @Bind
    private int LL;
    @Bind
    private double[] twKI;
    @Bind
    private double[] twKS;
    @Bind
    private double[] twINW;
    @Bind
    private double[] twEKS;
    @Bind
    private double[] twIMP;
    @Bind
    private double[] KI;
    @Bind
    private double[] KS;
    @Bind
    private double[] INW;
    @Bind
    private double[] EKS;
    @Bind
    private double[] IMP;
    @Bind
    private double[] PKB;

    public Model2() {}

    public void run() {
        PKB = new double[LL];

        for (int t = 0; t < LL; t++) {
            if (t > 0) {
                KI[t] = KI[t - 1] * (twKI[t] + 0.05) + Math.sin(t);
                KS[t] = KS[t - 1] * (twKS[t] - 0.03) - Math.cos(t);
                INW[t] = INW[t - 1] * (twINW[t] + 0.02) * Math.log(t + 1);
                EKS[t] = EKS[t - 1] * (twEKS[t] * 1.1) + Math.pow(t, 2);
                IMP[t] = IMP[t - 1] * (twIMP[t] * 0.95) + Math.sqrt(t);
            }

            PKB[t] = KI[t] + KS[t] + INW[t] + EKS[t] - IMP[t];
        }
    }
}
