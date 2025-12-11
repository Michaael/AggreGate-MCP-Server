package com.tibbo.aggregate.client.auth;

import java.security.spec.*;
import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.gui.dialog.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.component.*;

public class WorkspaceAuthenticator extends AbstractAuthenticator
{
    public WorkspaceAuthenticator(ClientCommandLineParameters parameters)
    {
        super(parameters);
    }

    @Override
    public Workspace authenticate() throws InvalidKeySpecException, AggreGateException
    {
        boolean authSuccess = false;
        while (!authSuccess)
        {
            if (ComponentHelper.getUsername() == null || ComponentHelper.getPassword() == null)
            {
                SplashAuthDialog saw = getSplashAuthDialog(null);

                saw.setUsername(ComponentHelper.getConfig().getLastUsername());

                if (saw.run() != SplashAuthDialog.OK)
                    if (!showStartupErrorAndExit(Pres.get().getString("mAuthCancelled")))
                        continue;

                Log.CORE.info("Authorized for workspace '" + saw.getUsername() + "'");

                ComponentHelper.setUsername(saw.getUsername());
                ComponentHelper.setPassword(saw.getPassword());
            }

            final Workspace loadedWorkspace = Client.getWorkspaceManager().loadWorkspace(ComponentHelper.getUsername(), ComponentHelper.getPassword());

            authSuccess = loadedWorkspace != null;

            if (!authSuccess)
            {
                JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), Pres.get().getString("mWorkspaceLoginFailed"));
                ComponentHelper.setPassword(null);
            } else
            {
                ComponentHelper.setPreferModalDialogs(loadedWorkspace.isPreferModalDialogs());
                ComponentHelper.setFrameDataStorage(loadedWorkspace);
                return loadedWorkspace;
            }
        }

        return null; // Should never happen
    }

    @Override
    protected int getMode()
    {
        return SplashAuthDialog.MODE_WORKSPACE;
    }

    @Override
    protected SplashAuthDialog getSplashAuthDialog(ClientCommandLineParameters parameters)
    {
        return new WorkspaceSplashAuthDialog(getMode());
    }
}
