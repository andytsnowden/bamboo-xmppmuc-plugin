package com.ipsoft.plugins.bamboo.xmppmuc;

import com.atlassian.bamboo.instantmessagingserver.InstantMessagingServerDefinition;
import com.atlassian.bamboo.instantmessagingserver.InstantMessagingServerDefinitionImpl;

public class XMPPMucConferenceInstantMessagingServerDefinition extends InstantMessagingServerDefinitionImpl {
    public XMPPMucConferenceInstantMessagingServerDefinition(InstantMessagingServerDefinition o) {
        super(o.getId(), o.getName(), o.getHost(), o.getPort(), o.getUsername(), o.getPassword(), o.getResource(), o.isEnforceLegacySsl(), o.isSecureConnectionRequired());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof XMPPMucConferenceInstantMessagingServerDefinition)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        XMPPMucConferenceInstantMessagingServerDefinition that = (XMPPMucConferenceInstantMessagingServerDefinition) o;
        if (this.isEnforceLegacySsl() != that.isEnforceLegacySsl()) {
            return false;
        }
        if (this.isSecureConnectionRequired() != that.isSecureConnectionRequired()) {
            return false;
        }
        if (this.getHost() != null ? !this.getHost().equals(that.getHost()) : that.getHost() != null) {
            return false;
        }
        if (this.name != null ? !this.name.equals(that.name) : that.name != null) {
            return false;
        }
        if (this.getPassword() != null ? !this.getPassword().equals(that.getPassword()) : that.getPassword() != null) {
            return false;
        }
        if (this.getPort() != null ? !this.getPort().equals(that.getPort()) : that.getPort() != null) {
            return false;
        }
        if (this.getResource() != null ? !this.getResource().equals(that.getResource()) : that.getResource() != null) {
            return false;
        }
        return this.getUsername() != null ? this.getUsername().equals(that.getUsername()) : that.getUsername() == null;
    }
}
