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
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.error.SimpleErrorCollection;
import com.atlassian.bamboo.variable.CustomVariableContext;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;


public class XMPPMucNotificationRecipient extends AbstractNotificationRecipient implements DeploymentResultAwareNotificationRecipient,
                                                                                            NotificationRecipient.RequiresPlan,
                                                                                            NotificationRecipient.RequiresResultSummary {

    private static String MUC_ROOM = "room";
    private static String MUC_ROOMPW = "roompw";
    private static String MUC_NICKNAME = "nickname";
    private String room = null;
    private String roompw = null;
    private String nickname = null;

    private TemplateRenderer templateRenderer;

    private ImmutablePlan plan;
    private ResultsSummary resultsSummary;
    private DeploymentResult deploymentResult;
    private CustomVariableContext customVariableContext;

    @Override
    public void populate(@NotNull Map<String, String[]> params)
    {
        this.room = getParam(MUC_ROOM, params);
        this.roompw = getParam(MUC_ROOMPW, params);
        this.nickname = getParam(MUC_NICKNAME, params);
    }

    @Override
    public void init(@Nullable String configurationData)
    {
        //Skip if there's nothing to process
        if (configurationData == null || configurationData.length() == 0){
            return;
        }

        SAXBuilder sb = new SAXBuilder();
        try {
            Document doc = sb.build(new StringReader(configurationData));
            Element root = doc.getRootElement();

            room = root.getChildText(MUC_ROOM);
            roompw = root.getChildText(MUC_ROOMPW);
            nickname = root.getChildText(MUC_NICKNAME);
        } catch (JDOMException e){
            //Ignore
        } catch (IOException e) {
            //Ignore
        }
    }

    @NotNull
    @Override
    public String getRecipientConfig()
    {
        //Root
        Document doc = new Document();
        Element root = new Element(XMPPMucNotificationRecipient.class.getName());
        doc.addContent(root);

        //Room
        Element xmlRoom = new Element(MUC_ROOM);
        if (this.room != null) xmlRoom.setText(this.room);
        root.addContent(xmlRoom);

        //Room PW
        Element xmlRoompw = new Element(MUC_ROOMPW);
        if (this.roompw != null) xmlRoompw.setText(this.roompw);
        root.addContent(xmlRoompw);

        //Room NickName
        Element xmlNicname = new Element(MUC_NICKNAME);
        if (this.nickname != null) xmlNicname.setText(this.nickname);
        root.addContent(xmlNicname);

        //Serialize
        Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setOmitDeclaration(true);
        XMLOutputter outputter = new XMLOutputter(prettyFormat);
        String xmlString = outputter.outputString(doc);

        return xmlString;
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
        if (nickname != null)
        {
            context.put(MUC_NICKNAME, nickname);
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
        list.add(new XMPPMucNotificationTransport(room, roompw, nickname, plan, resultsSummary, deploymentResult, customVariableContext));
        return list;
    }

    @Override
    public ErrorCollection validate(@NotNull Map<String, String[]> params) {
        ErrorCollection errorCollection = new SimpleErrorCollection();

        //MUC_ROOM is the only required field
        String[] roomArray = (String[]) params.get(MUC_ROOM);
        if ((roomArray == null) || (roomArray.length == 0)) {
            errorCollection.addError(MUC_ROOM, "You must enter a MUC room JID");
            return errorCollection;
        }

        //Valid MUC_ROOM JID Format
        this.room = getParam(MUC_ROOM, params);
        if (!this.room.matches(".*@.*")){
            errorCollection.addError(MUC_ROOM, "Invalid format, should be roomname@conferance-server-url");
            return errorCollection;
        }

        return errorCollection;
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
