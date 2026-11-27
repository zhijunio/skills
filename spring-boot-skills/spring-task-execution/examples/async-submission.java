// Illustrative caller; reportTasks is an injected Spring proxy.
public CompletableFuture<String> submit(UUID reportId) {
    try {
        return reportTasks.render(reportId).whenComplete((result, failure) -> {
            if (failure != null) recordFailure(reportId, failure);
            else recordResult(reportId, result);
        });
    } catch (TaskRejectedException overloaded) {
        rejectSubmission(reportId, overloaded); // map to the existing retryable API contract
        return CompletableFuture.failedFuture(overloaded);
    }
}
// This is process-local work, not durable acceptance. Callback failures complete the returned future exceptionally.
