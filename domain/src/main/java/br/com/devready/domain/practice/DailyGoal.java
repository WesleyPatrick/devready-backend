package br.com.devready.domain.practice;

public record DailyGoal(int done, int target) {

    public static final int TARGET = 3;

    public DailyGoal {
        if (target < 1 || done < 0 || done > target) {
            throw new PracticeException("Meta do dia inválida: " + done + " de " + target + ".");
        }
    }

    public static DailyGoal of(int activitiesToday) {
        return new DailyGoal(Math.min(TARGET, activitiesToday), TARGET);
    }
}
