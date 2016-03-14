package com.ipsoft.plugins.bamboo.xmppmuc;


import com.atlassian.bamboo.deployments.notification.DeploymentResultAwareNotificationRecipient;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.NotificationRecipient;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.notification.recipients.AbstractNotificationRecipient;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plugin.descriptor.NotificationRecipientModuleDescriptor;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;


public class XMPPMucNotificationRecipient extends AbstractNotificationRecipient implements DeploymentResultAwareNotificationRecipient,
                                                                                            NotificationRecipient.RequiresPlan,
                                                                                            NotificationRecipient.RequiresResultSummary {

    private static String MUC_ROOM = "room";
    private static String MUC_ROOMPW = "roompw";
    private String room = null;
    private String roompw = null;

    private TemplateRenderer templateRenderer;

    private ImmutablePlan plan;
    private ResultsSummary resultsSummary;
    private DeploymentResult deploymentResult;
    private CustomVariableContext customVariableContext;

    @Override
    public void populate(@NotNull Map<String, String[]> params)
    {
        for (String next : params.keySet())
        {
            System.out.println("next = " + next);
        }
        if (params.containsKey(MUC_ROOM))
        {
            int i = params.get(MUC_ROOM).length - 1;
            this.room = params.get(MUC_ROOM)[i];
        }
        if (params.containsKey(MUC_ROOMPW))
        {
            int i = params.get(MUC_ROOMPW).length - 1;
            this.roompw = params.get(MUC_ROOMPW)[i];
        }
    }

    @Override
    public void init(@Nullable String configurationData)
    {
        if (StringUtils.isNotBlank(configurationData))
        {
            String delimiter = "\\|";

            String[] configValues = configurationData.split(delimiter);

            if (configValues.length > 0) {
                room = configValues[0];
            }
            if (configValues.length > 1) {
                roompw = configValues[1];
            }
        }
    }

    @NotNull
    @Override
    public String getRecipientConfig()
    {
        String delimiter = "|";

        StringBuilder recipientConfig = new StringBuilder();
        if (StringUtils.isNotBlank(room)) {
            recipientConfig.append(room);
        }
        if (StringUtils.isNotBlank(roompw)) {
            recipientConfig.append(delimiter);
            recipientConfig.append(roompw);
        }
        return recipientConfig.toString();
    }

    @NotNull
    @Override
    public String getEditHtml()
    {
        String editTemplateLocation = ((NotificationRecipientModuleDescriptor)getModuleDescriptor()).getEditTemplate();
        return templateRenderer.render(editTemplateLocation, populateContext());
    }

    private Map<String, Object> populateContext()
    {
        Map<String, Object> context = Maps.newHashMap();

        if (room != null)
        {
            context.put(MUC_ROOM, room);
        }
        if (roompw != null)
        {
            context.put(MUC_ROOMPW, roompw);
        }

        System.out.println("populateContext = " + context.toString());

        return context;
    }

    @NotNull
    @Override
    public String getViewHtml()
    {
        String editTemplateLocation = ((NotificationRecipientModuleDescriptor)getModuleDescriptor()).getViewTemplate();
        return templateRenderer.render(editTemplateLocation, populateContext());
    }

    @NotNull
    public List<NotificationTransport> getTransports() {
        List<NotificationTransport> list = Lists.newArrayList();
        list.add(new XMPPMucNotificationTransport(room, roompw, plan, resultsSummary, deploymentResult, customVariableContext));
        return list;
    }

    public void setDeploymentResult(DeploymentResult deploymentResult) {
        this.deploymentResult = deploymentResult;
    }

    public void setPlan(final @Nullable Plan plan)
    {
        this.plan = plan;
    }

    public void setPlan(@Nullable final ImmutablePlan plan)
    {
        this.plan = plan;
    }

    public void setResultsSummary(ResultsSummary resultsSummary) {
        this.resultsSummary = resultsSummary;
    }

    //-----------------------------------Dependencies
    public void setTemplateRenderer(TemplateRenderer templateRenderer)
    {
        this.templateRenderer = templateRenderer;
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) { this.customVariableContext = customVariableContext; }
}
