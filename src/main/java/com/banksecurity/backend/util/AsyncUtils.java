package com.banksecurity.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Utilitaire pour l'exécution de tâches asynchrones
 */
@Slf4j
public final class AsyncUtils {

    private AsyncUtils() {
        throw new IllegalStateException("Classe utilitaire - ne pas instancier");
    }

    /**
     * Exécute une tâche de manière asynchrone avec un executor spécifique
     */
    public static <T> CompletableFuture<T> runAsync(Supplier<T> task, Executor executor, String taskName) {
        log.debug("Démarrage de la tâche asynchrone: {}", taskName);

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                T result = task.get();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Tâche {} terminée en {} ms", taskName, duration);
                return result;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("Tâche {} échouée en {} ms: {}", taskName, duration, e.getMessage());
                throw e;
            }
        }, executor);
    }

    /**
     * Exécute une tâche de manière asynchrone sans valeur de retour
     */
    public static CompletableFuture<Void> runAsync(Runnable task, Executor executor, String taskName) {
        log.debug("Démarrage de la tâche asynchrone: {}", taskName);

        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                task.run();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Tâche {} terminée en {} ms", taskName, duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("Tâche {} échouée en {} ms: {}", taskName, duration, e.getMessage());
                throw e;
            }
        }, executor);
    }

    /**
     * Exécute plusieurs tâches en parallèle et attend leur complétion
     */
    public static <T> CompletableFuture<T[]> allOf(CompletableFuture<T>... futures) {
        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    @SuppressWarnings("unchecked")
                    T[] results = (T[]) new Object[futures.length];
                    for (int i = 0; i < futures.length; i++) {
                        results[i] = futures[i].join();
                    }
                    return results;
                });
    }
}