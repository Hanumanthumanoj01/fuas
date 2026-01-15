package com.frauas.servicemanagement.service;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CamundaProcessService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    // ✅ FIX: Accept variables map directly
    public String startServiceRequestProcess(Map<String, Object> variables) {

        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey(
                        "service-request-process",
                        variables
                );

        return processInstance.getId();
    }

    public List<Task> getTasksForAssignee(String assignee) {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }

    public List<Task> getAllActiveTasks() {
        return taskService.createTaskQuery()
                .active()
                .list();
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, variables);
    }

    public Task getTaskById(String taskId) {
        return taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
    }

    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }
}
