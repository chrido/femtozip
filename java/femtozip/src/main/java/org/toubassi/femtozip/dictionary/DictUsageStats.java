package org.toubassi.femtozip.dictionary;


public class DictUsageStats {
    private final double notUsedPercent;
    private final double usedPercent;
    private final int internalUsage;

    public DictUsageStats(double notUsedPercent, double usedPercent, int internalUsage) {
        this.notUsedPercent = notUsedPercent;
        this.usedPercent = usedPercent;

        this.internalUsage = internalUsage;
    }

    public double getNotUsedPercent() {
        return notUsedPercent;
    }

    public double getUsedPercent() {
        return usedPercent;
    }

    public int getInternalUsage() {
        return internalUsage;
    }
}
