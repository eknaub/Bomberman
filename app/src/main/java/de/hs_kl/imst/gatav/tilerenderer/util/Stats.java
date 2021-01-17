package de.hs_kl.imst.gatav.tilerenderer.util;

public class Stats {
    public static int examPlacedCounter = 0;
    public static int studentsTotal = 0;
    public static int studentsLeft = 0;
    public static double time = 0.0;

    public static void incrementExamPlaced() { examPlacedCounter++; }
    public static void setStudentsTotal(int _studentsTotal) { studentsTotal = _studentsTotal; }
    public static void setStudentsLeft(int _studentsLeft) { studentsLeft = _studentsLeft; }
    public static void setTime(double _time) { time = _time; }

    public static void reset() {
        examPlacedCounter = 0;
        studentsTotal = 0;
        studentsLeft = 0;
        time = 0.0;
    }
}
