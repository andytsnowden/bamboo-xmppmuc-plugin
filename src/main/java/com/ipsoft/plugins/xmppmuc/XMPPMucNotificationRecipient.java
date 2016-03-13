package com.ipsoft.plugins.xmppmuc;


import com.atlassian.bamboo.deployments.notification.DeploymentResultAwareNotificationRecipient;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.NotificationRecipient;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.notification.recipients.AbstractNotificationRecipient;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;

/**
 * Created by MrRx7 on 3/12/2016.
 */

public class XMPPMucNotificationRecipient extends AbstractNotificationRecipient implements DeploymentResultAwareNotificationRecipient,
                                                                                            NotificationRecipient.RequiresPlan,
                                                                                            NotificationRecipient.RequiresResultSummary {

    private ImmutablePlan plan;
    private ResultsSummary resultsSummary;
    private DeploymentResult deploymentResult;
    private CustomVariableContext customVariableContext;


    @NotNull
    public List<NotificationTransport> getTransports() {
        List<NotificationTransport> list = Lists.newArrayList();
        //list.add(new SlackNotificationTransport(webhookUrl, channel, iconUrl, plan, resultsSummary, deploymentResult, customVariableContext));
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
}
