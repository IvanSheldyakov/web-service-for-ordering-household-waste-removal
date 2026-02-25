package ru.nsu.waste.removal.ordering.service.core.service.reward;

/**
 * Чистая реализация "optimized gamification" (Lieder et al.).
 *
 * Модель:
 *  - s in [0..1] — сила привычки
 *  - alpha in (0..1) — скорость формирования/угасания
 *  - theta in (0..1) — порог "привычка сформирована"
 *  - M > 0 — масштаб очков
 */
public final class LiederOptimizedRewardCalculator {

    private final double alpha;
    private final double theta;
    private final int maxPoints;

    public LiederOptimizedRewardCalculator(double alpha, double theta, int maxPoints) {
        if (!(alpha > 0.0 && alpha < 1.0)) {
            throw new IllegalArgumentException("alpha must be in (0,1)");
        }
        if (!(theta > 0.0 && theta < 1.0)) {
            throw new IllegalArgumentException("theta must be in (0,1)");
        }
        if (maxPoints <= 0) {
            throw new IllegalArgumentException("maxPoints must be > 0");
        }
        this.alpha = alpha;
        this.theta = theta;
        this.maxPoints = maxPoints;
    }

    public StepResult step(double habitStrength, boolean success) {
        double s = clamp01(habitStrength);

        // f(s, 1) = 1 - s
        // f(s, 0) = V*(s(1-alpha)) - V*(s)
        double f = success ? (1.0 - s) : (vStar(s * (1.0 - alpha)) - vStar(s));
        int pointsDelta = (int) Math.round(maxPoints * f);

        double newStrength = success
                ? (s + alpha * (1.0 - s))
                : (s * (1.0 - alpha));

        return new StepResult(pointsDelta, clamp01(newStrength), f);
    }

    /**
     * V*(s) = r_goal - (1-s) * sum_{i=1..n(s;theta)} (1-alpha)^{i-1}
     * В разности V*(s') - V*(s) константа r_goal сокращается => можно считать r_goal = 0.
     */
    private double vStar(double s) {
        s = clamp01(s);
        int n = stepsToReachTheta(s);
        if (n <= 0) {
            return 0.0;
        }

        double base = 1.0 - alpha;
        // геометрическая сумма: 1 + base + ... + base^(n-1) = (1 - base^n) / (1 - base) = (1 - base^n) / alpha
        double sum = (1.0 - Math.pow(base, n)) / alpha;
        return -(1.0 - s) * sum;
    }

    /**
     * n(s;theta): минимальное число успехов, чтобы по динамике
     * s_{t+1} = s_t + alpha(1 - s_t)
     * достигнуть theta.
     */
    private int stepsToReachTheta(double s) {
        if (s >= theta) {
            return 0;
        }
        double base = 1.0 - alpha;
        double ratio = (1.0 - theta) / (1.0 - s);
        double nReal = Math.log(ratio) / Math.log(base);
        int n = (int) Math.ceil(nReal - 1e-12);
        return Math.max(n, 1);
    }

    private static double clamp01(double x) {
        if (x < 0.0) {
            return 0.0;
        }
        if (x > 1.0) {
            return 1.0;
        }
        return x;
    }

    public record StepResult(
            int pointsDelta,
            double newStrength,
            double fValue
    ) {
    }
}