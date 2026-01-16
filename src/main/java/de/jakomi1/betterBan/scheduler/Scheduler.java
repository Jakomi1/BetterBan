package de.jakomi1.betterBan.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static de.jakomi1.betterBan.BetterBan.plugin;

/**
 * Folia-aware Scheduler wrapper für Paper / Folia (1.21.x).
 * Verbesserte Variante: stellt sicher, dass Folia-runAtFixedRate nie mit delay <= 0 aufgerufen wird.
 */
public final class Scheduler {

    private Scheduler() { /* utility */ }

    // Prüfe zur Laufzeit, ob Folia-Style Scheduler API genutzt werden kann
    private static final boolean FOLIA_AVAILABLE;
    static {
        boolean v = false;
        try {
            Method m1 = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Method m2 = Bukkit.class.getMethod("getAsyncScheduler");
            v = (m1 != null) && (m2 != null);
        } catch (NoSuchMethodException ignored) {
            v = false;
        } catch (Throwable ignored) {
            v = false;
        }
        FOLIA_AVAILABLE = v;
    }

    public static final class TaskHandle {
        private final BukkitTask bukkitTask;
        private final Object foliaScheduledTask; // typed as Object to avoid compile-time dependency

        private TaskHandle(BukkitTask bukkitTask, Object foliaScheduledTask) {
            this.bukkitTask = bukkitTask;
            this.foliaScheduledTask = foliaScheduledTask;
        }

        public static TaskHandle fromBukkit(BukkitTask task) {
            return new TaskHandle(task, null);
        }

        public static TaskHandle fromFolia(Object task) {
            return new TaskHandle(null, task);
        }

        public void cancel() {
            if (bukkitTask != null) {
                bukkitTask.cancel();
            } else if (foliaScheduledTask != null) {
                try {
                    Method m = foliaScheduledTask.getClass().getMethod("cancel");
                    m.invoke(foliaScheduledTask);
                } catch (NoSuchMethodException nsme) {
                    try {
                        Method m2 = foliaScheduledTask.getClass().getMethod("stop");
                        m2.invoke(foliaScheduledTask);
                    } catch (Throwable ignored) { /* no-op */ }
                } catch (Throwable ignored) { /* no-op */ }
            }
        }

        public boolean isCancelled() {
            if (bukkitTask != null) return bukkitTask.isCancelled();
            if (foliaScheduledTask != null) {
                try {
                    Method isCancelled = foliaScheduledTask.getClass().getMethod("isCancelled");
                    Object res = isCancelled.invoke(foliaScheduledTask);
                    if (res instanceof Boolean) return (Boolean) res;
                } catch (NoSuchMethodException ignored) { /* try other */ }
                catch (Throwable ignored) { /* try other */ }

                try {
                    Method state = foliaScheduledTask.getClass().getMethod("getExecutionState");
                    Object st = state.invoke(foliaScheduledTask);
                    if (st == null) return true;
                    String s = String.valueOf(st).toLowerCase();
                    return s.contains("cancel") || s.contains("finished") || s.contains("terminated");
                } catch (NoSuchMethodException ignored) { /* give up */ }
                catch (Throwable ignored) { /* give up */ }

                return false;
            }
            return false;
        }

        @Override
        public String toString() {
            if (bukkitTask != null) return "TaskHandle[bukkit id=" + bukkitTask.getTaskId() + "]";
            if (foliaScheduledTask != null) return "TaskHandle[folia=" + foliaScheduledTask + "]";
            return "TaskHandle[none]";
        }
    }

    private static long ticksToMillis(long ticks) {
        return Math.max(0L, ticks) * 50L; // 1 Tick = 50 ms
    }

    /* --- Methoden --- */

    public static TaskHandle run(Runnable runnable) {
        Objects.requireNonNull(runnable);
        if (!FOLIA_AVAILABLE) {
            if (Bukkit.isPrimaryThread()) {
                runnable.run();
                return TaskHandle.fromBukkit(Bukkit.getScheduler().runTask(plugin, () -> {}));
            } else {
                return TaskHandle.fromBukkit(Bukkit.getScheduler().runTask(plugin, runnable));
            }
        } else {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Consumer<Object> consumer = (ignored) -> runnable.run();
                Method runMethod = globalScheduler.getClass().getMethod("run", Plugin.class, Consumer.class);
                Object scheduledTask = runMethod.invoke(globalScheduler, plugin, consumer);
                return TaskHandle.fromFolia(scheduledTask);
            } catch (ReflectiveOperationException e) {
                if (Bukkit.isPrimaryThread()) {
                    runnable.run();
                    return TaskHandle.fromBukkit(Bukkit.getScheduler().runTask(plugin, () -> {}));
                } else {
                    return TaskHandle.fromBukkit(Bukkit.getScheduler().runTask(plugin, runnable));
                }
            }
        }
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        Objects.requireNonNull(runnable);
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!FOLIA_AVAILABLE) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try { runnable.run(); future.complete(null); } catch (Throwable t) { future.completeExceptionally(t); }
            });
        } else {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Consumer<Object> consumer = (st) -> {
                    try { runnable.run(); future.complete(null); } catch (Throwable t) { future.completeExceptionally(t); }
                };
                Method runNow = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                runNow.invoke(asyncScheduler, plugin, consumer);
            } catch (ReflectiveOperationException e) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try { runnable.run(); future.complete(null); } catch (Throwable t) { future.completeExceptionally(t); }
                });
            }
        }
        return future;
    }

    public static TaskHandle runLater(Runnable runnable, long delayTicks) {
        Objects.requireNonNull(runnable);
        long delay = Math.max(1L, delayTicks);
        if (!FOLIA_AVAILABLE) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
            return TaskHandle.fromBukkit(task);
        } else {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Consumer<Object> consumer = (st) -> runnable.run();
                Method m = globalScheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                // runDelayed accepts delay in ticks in some versions; keep as-is
                Object scheduledTask = m.invoke(globalScheduler, plugin, consumer, delay);
                return TaskHandle.fromFolia(scheduledTask);
            } catch (ReflectiveOperationException e) {
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
                return TaskHandle.fromBukkit(task);
            }
        }
    }

    public static TaskHandle runLaterAsync(Runnable runnable, long delayTicks) {
        Objects.requireNonNull(runnable);
        long delay = Math.max(0L, delayTicks);
        if (!FOLIA_AVAILABLE) {
            BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
            return TaskHandle.fromBukkit(task);
        } else {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Consumer<Object> consumer = (st) -> runnable.run();
                long delayMillis = ticksToMillis(delay);
                Method m = asyncScheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
                Object scheduledTask = m.invoke(asyncScheduler, plugin, consumer, delayMillis, TimeUnit.MILLISECONDS);
                return TaskHandle.fromFolia(scheduledTask);
            } catch (ReflectiveOperationException e) {
                BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
                return TaskHandle.fromBukkit(task);
            }
        }
    }

    public static TaskHandle runRepeating(Runnable runnable, long delayTicks, long periodTicks) {
        Objects.requireNonNull(runnable);
        long delay = Math.max(0L, delayTicks);
        long period = Math.max(1L, periodTicks);

        if (!FOLIA_AVAILABLE) {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
            return TaskHandle.fromBukkit(task);
        } else {
            try {
                // Folia verlangt initial delay > 0 für runAtFixedRate
                long foliaDelay = Math.max(1L, delay);
                long foliaPeriod = Math.max(1L, period);
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Consumer<Object> consumer = (st) -> runnable.run();
                Method m = globalScheduler.getClass()
                        .getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                Object scheduledTask = m.invoke(globalScheduler, plugin, consumer, foliaDelay, foliaPeriod);
                return TaskHandle.fromFolia(scheduledTask);
            } catch (ReflectiveOperationException e) {
                // Falls Reflection fehlschlägt, fallback auf Bukkit (Achtung: auf Folia kann das UnsupportedOperationException werfen)
                try {
                    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
                    return TaskHandle.fromBukkit(task);
                } catch (Throwable t) {
                    t.printStackTrace();
                    return TaskHandle.fromBukkit(null);
                }
            }
        }
    }

    public static TaskHandle runTimer(Runnable runnable, long delayTicks, long periodTicks, long iterations) {
        Objects.requireNonNull(runnable);
        if (iterations <= 0) throw new IllegalArgumentException("iterations must be > 0");
        long delay = Math.max(0L, delayTicks);
        long period = Math.max(1L, periodTicks);

        if (!FOLIA_AVAILABLE) {
            AtomicInteger counter = new AtomicInteger(0);
            AtomicReference<BukkitTask> ref = new AtomicReference<>();
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                int idx = counter.getAndIncrement();
                try { runnable.run(); } catch (Throwable t) { t.printStackTrace(); }
                if (idx + 1 >= iterations) {
                    BukkitTask tRef = ref.get();
                    if (tRef != null) tRef.cancel();
                }
            }, delay, period);
            ref.set(task);
            return TaskHandle.fromBukkit(task);
        } else {
            try {
                long foliaDelay = Math.max(1L, delay);
                long foliaPeriod = Math.max(1L, period);
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                final AtomicInteger counter = new AtomicInteger(0);
                Consumer<Object> consumer = (scheduledTask) -> {
                    int idx = counter.getAndIncrement();
                    try { runnable.run(); } catch (Throwable t) { t.printStackTrace(); }
                    if (idx + 1 >= iterations) {
                        try {
                            Method cancel = scheduledTask.getClass().getMethod("cancel");
                            cancel.invoke(scheduledTask);
                        } catch (ReflectiveOperationException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Method m = globalScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                Object scheduledTask = m.invoke(globalScheduler, plugin, consumer, foliaDelay, foliaPeriod);
                return TaskHandle.fromFolia(scheduledTask);
            } catch (ReflectiveOperationException e) {
                // Fallback
                AtomicInteger counter = new AtomicInteger(0);
                AtomicReference<BukkitTask> ref = new AtomicReference<>();
                BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    int idx = counter.getAndIncrement();
                    try { runnable.run(); } catch (Throwable t) { t.printStackTrace(); }
                    if (idx + 1 >= iterations) {
                        BukkitTask tRef = ref.get();
                        if (tRef != null) tRef.cancel();
                    }
                }, delay, period);
                ref.set(task);
                return TaskHandle.fromBukkit(task);
            }
        }
    }

    public static TaskHandle runTimer(IntConsumer consumer, long delayTicks, long periodTicks, int iterations) {
        Objects.requireNonNull(consumer);
        if (iterations <= 0) throw new IllegalArgumentException("iterations must be > 0");
        long delay = Math.max(0L, delayTicks);
        long period = Math.max(1L, periodTicks);

        if (!FOLIA_AVAILABLE) {
            AtomicInteger counter = new AtomicInteger(0);
            AtomicReference<BukkitTask> ref = new AtomicReference<>();
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                int idx = counter.getAndIncrement();
                try { consumer.accept(idx); } catch (Throwable t) { t.printStackTrace(); }
                if (idx + 1 >= iterations) {
                    BukkitTask tRef = ref.get();
                    if (tRef != null) tRef.cancel();
                }
            }, delay, period);
            ref.set(task);
            return TaskHandle.fromBukkit(task);
        } else {
            try {
                long foliaDelay = Math.max(1L, delay);
                long foliaPeriod = Math.max(1L, period);
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                final AtomicInteger counter = new AtomicInteger(0);
                Consumer<Object> foliaConsumer = (scheduledTask) -> {
                    int idx = counter.getAndIncrement();
                    try { consumer.accept(idx); } catch (Throwable t) { t.printStackTrace(); }
                    if (idx + 1 >= iterations) {
                        try {
                            Method cancel = scheduledTask.getClass().getMethod("cancel");
                            cancel.invoke(scheduledTask);
                        } catch (ReflectiveOperationException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Method m = globalScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                Object scheduledTask = m.invoke(globalScheduler, plugin, foliaConsumer, foliaDelay, foliaPeriod);
                return TaskHandle.fromFolia(scheduledTask);
            } catch (ReflectiveOperationException e) {
                AtomicInteger counter = new AtomicInteger(0);
                AtomicReference<BukkitTask> ref = new AtomicReference<>();
                BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    int idx = counter.getAndIncrement();
                    try { consumer.accept(idx); } catch (Throwable t) { t.printStackTrace(); }
                    if (idx + 1 >= iterations) {
                        BukkitTask tRef = ref.get();
                        if (tRef != null) tRef.cancel();
                    }
                }, delay, period);
                ref.set(task);
                return TaskHandle.fromBukkit(task);
            }
        }
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        CompletableFuture<T> future = new CompletableFuture<>();
        if (!FOLIA_AVAILABLE) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try { T result = supplier.get(); future.complete(result); } catch (Throwable t) { future.completeExceptionally(t); }
            });
        } else {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Consumer<Object> consumer = (st) -> {
                    try { T result = supplier.get(); future.complete(result); } catch (Throwable t) { future.completeExceptionally(t); }
                };
                Method runNow = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                runNow.invoke(asyncScheduler, plugin, consumer);
            } catch (ReflectiveOperationException e) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try { T result = supplier.get(); future.complete(result); } catch (Throwable t) { future.completeExceptionally(t); }
                });
            }
        }
        return future;
    }

    public static void runAsyncThenSync(Runnable asyncPart, Runnable syncAfter) {
        Objects.requireNonNull(asyncPart);
        Objects.requireNonNull(syncAfter);
        if (!FOLIA_AVAILABLE) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try { asyncPart.run(); } finally { Bukkit.getScheduler().runTask(plugin, syncAfter); }
            });
        } else {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Consumer<Object> consumer = (st) -> {
                    try { asyncPart.run(); } finally {
                        try {
                            Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                            Method exec = globalScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                            exec.invoke(globalScheduler, plugin, syncAfter);
                        } catch (ReflectiveOperationException ex) {
                            Bukkit.getScheduler().runTask(plugin, syncAfter);
                        }
                    }
                };
                Method runNow = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                runNow.invoke(asyncScheduler, plugin, consumer);
            } catch (ReflectiveOperationException e) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try { asyncPart.run(); } finally { Bukkit.getScheduler().runTask(plugin, syncAfter); }
                });
            }
        }
    }

    public static void removeEntitySafe(Entity entity) {
        if (entity == null) return;
        if (!FOLIA_AVAILABLE) {
            if (Bukkit.isPrimaryThread()) {
                if (!entity.isDead()) entity.remove();
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> { if (!entity.isDead()) entity.remove(); });
            }
            return;
        }

        try {
            Method getScheduler = entity.getClass().getMethod("getScheduler");
            Object entityScheduler = getScheduler.invoke(entity);
            if (entityScheduler != null) {
                try {
                    Method exec = entityScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
                    exec.invoke(entityScheduler, plugin, (Runnable) () -> { if (!entity.isDead()) entity.remove(); }, (Runnable) () -> {}, 0L);
                    return;
                } catch (NoSuchMethodException nsme) {
                    try {
                        Method exec2 = entityScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                        exec2.invoke(entityScheduler, plugin, (Runnable) () -> { if (!entity.isDead()) entity.remove(); });
                        return;
                    } catch (NoSuchMethodException nsme2) { /* fallback */ }
                }
            }
        } catch (ReflectiveOperationException ignored) { /* fallback */ }

        try {
            if (Bukkit.isPrimaryThread()) {
                if (!entity.isDead()) entity.remove();
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> { if (!entity.isDead()) entity.remove(); });
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
