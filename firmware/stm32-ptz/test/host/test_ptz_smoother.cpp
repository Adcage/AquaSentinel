#include <cassert>
#include <cstdio>
#include <cstdint>

namespace ptz_config {
constexpr uint16_t PTZ_SMOOTH_CRUISE_US = 60;
constexpr uint16_t PTZ_SMOOTH_ACCEL_US = 15;
constexpr uint16_t PTZ_SMOOTH_DECEL_ZONE_US = 300;
constexpr uint16_t PTZ_SMOOTH_DEAD_ZONE_US = 8;
constexpr uint32_t PTZ_SMOOTH_INTERVAL_MS = 20;
}  // namespace ptz_config

struct SmoothResult {
    int steps;
    bool converged;
    int maxVelocity;
    bool decelerated;
};

static SmoothResult simulateSmooth(uint16_t startUs, uint16_t targetUs) {
    uint16_t currentUs = startUs;
    int16_t velocity = 0;
    int steps = 0;
    int maxVel = 0;
    bool sawDeceleration = false;
    int prevAbsVel = 0;

    while (steps < 400) {
        int32_t error = static_cast<int32_t>(targetUs) - static_cast<int32_t>(currentUs);
        int32_t absError = (error < 0) ? -error : error;

        if (absError <= ptz_config::PTZ_SMOOTH_DEAD_ZONE_US) {
            return {steps, true, maxVel, sawDeceleration};
        }

        int32_t direction = (error > 0) ? 1 : -1;
        int32_t targetSpeed;

        if (absError <= static_cast<int32_t>(ptz_config::PTZ_SMOOTH_DECEL_ZONE_US)) {
            targetSpeed = absError * static_cast<int32_t>(ptz_config::PTZ_SMOOTH_CRUISE_US) /
                          static_cast<int32_t>(ptz_config::PTZ_SMOOTH_DECEL_ZONE_US);
            if (targetSpeed < 1) targetSpeed = 1;
        } else {
            targetSpeed = static_cast<int32_t>(ptz_config::PTZ_SMOOTH_CRUISE_US);
        }

        int32_t absVel = (velocity < 0) ? -velocity : velocity;
        if (absVel < targetSpeed) {
            velocity += static_cast<int16_t>(ptz_config::PTZ_SMOOTH_ACCEL_US * direction);
            absVel += ptz_config::PTZ_SMOOTH_ACCEL_US;
            if (absVel > targetSpeed) {
                velocity = static_cast<int16_t>(targetSpeed * direction);
            }
        } else if (absVel > targetSpeed) {
            velocity = static_cast<int16_t>(targetSpeed * direction);
        }

        int curAbsVel = (velocity < 0) ? -velocity : velocity;
        if (curAbsVel > maxVel) maxVel = curAbsVel;
        if (steps > 2 && curAbsVel < prevAbsVel) sawDeceleration = true;
        prevAbsVel = curAbsVel;

        currentUs = static_cast<uint16_t>(static_cast<int32_t>(currentUs) + velocity);
        steps++;
    }

    return {steps, false, maxVel, sawDeceleration};
}

int main() {
    // 全行程 500->2500us
    SmoothResult r1 = simulateSmooth(500, 2500);
    assert(r1.converged);
    assert(r1.steps <= 250);
    assert(r1.maxVelocity == ptz_config::PTZ_SMOOTH_CRUISE_US);
    assert(r1.decelerated);
    printf("500->2500us: steps=%d, maxVel=%d, decel=%d\n", r1.steps, r1.maxVelocity, r1.decelerated);

    // 反向全行程
    SmoothResult r2 = simulateSmooth(2500, 500);
    assert(r2.converged);
    assert(r2.decelerated);
    printf("2500->500us: steps=%d, decel=%d\n", r2.steps, r2.decelerated);

    // 小行程 1500->1600us
    SmoothResult r3 = simulateSmooth(1500, 1600);
    assert(r3.converged);
    printf("1500->1600us: steps=%d, maxVel=%d\n", r3.steps, r3.maxVelocity);

    // 已在目标
    SmoothResult r4 = simulateSmooth(1500, 1500);
    assert(r4.converged);
    assert(r4.steps == 0);
    printf("1500->1500us: steps=%d\n", r4.steps);

    // 死区边界
    SmoothResult r5 = simulateSmooth(1498, 1500);
    assert(r5.converged);
    assert(r5.steps == 0);
    printf("1498->1500us: steps=%d (dead zone)\n", r5.steps);

    // 刚超死区：100us 行程（在减速区内）
    SmoothResult r6 = simulateSmooth(1400, 1500);
    assert(r6.converged);
    assert(r6.decelerated);
    printf("1400->1500us: steps=%d, decel=%d\n", r6.steps, r6.decelerated);

    // 中等行程：验证有加速段
    SmoothResult r7 = simulateSmooth(500, 1500);
    assert(r7.converged);
    assert(r7.maxVelocity == ptz_config::PTZ_SMOOTH_CRUISE_US);
    assert(r7.decelerated);
    printf("500->1500us: steps=%d, maxVel=%d, decel=%d\n", r7.steps, r7.maxVelocity, r7.decelerated);

    printf("All PTZ trapezoidal smoother tests passed.\n");
    return 0;
}
