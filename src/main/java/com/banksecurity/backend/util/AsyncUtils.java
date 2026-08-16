package com.banksecurity.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
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
     * Le CompletableFuture retourné doit être utilisé par l'appelant (ex: .join())
     */
    public static <T> CompletableFuture<T> runAsync(Supplier<T> task, Executor executor, String taskName) {
        log.debug("[ASYNC-SUPPLIER] Démarrage de la tâche: {}", taskName);

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                T result = task.get();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("[ASYNC-SUPPLIER] Succès de la tâche: {} (durée: {} ms)", taskName, duration);
                return result;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("[ASYNC-SUPPLIER] Échec de la tâche: {} (durée: {} ms, erreur: {})",
                        taskName, duration, e.getMessage());
                throw e;
            }
        }, executor);
    }

    /**
     * Exécute une tâche de manière asynchrone sans valeur de retour
     * Le CompletableFuture retourné doit être utilisé par l'appelant (ex: .join())
     */
    public static CompletableFuture<Void> runAsync(Runnable task, Executor executor, String taskName) {
        log.debug("[ASYNC-RUNNABLE] Lancement de l'exécution: {}", taskName);

        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                task.run();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("[ASYNC-RUNNABLE] Exécution réussie: {} (durée: {} ms)", taskName, duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("[ASYNC-RUNNABLE] Exécution échouée: {} (durée: {} ms, raison: {})",
                        taskName, duration, e.getMessage());
                throw e;
            }
        }, executor);
    }

    /**
     * Exécute plusieurs tâches en parallèle et attend leur complétion
     * Le CompletableFuture retourné doit être utilisé par l'appelant (ex: .join())
     */
    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
        log.debug("[ASYNC-ALLOF-LIST] Attente de {} futures depuis une liste", futures.size());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<T> results = new java.util.ArrayList<>(futures.size());
                    for (CompletableFuture<T> future : futures) {
                        results.add(future.join());
                    }
                    log.debug("[ASYNC-ALLOF-LIST] {} résultats collectés avec succès", results.size());
                    return results;
                });
    }
}