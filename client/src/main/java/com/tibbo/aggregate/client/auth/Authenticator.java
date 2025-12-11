package com.tibbo.aggregate.client.auth;

import java.security.spec.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.common.*;

public interface Authenticator
{
  Workspace authenticate() throws InvalidKeySpecException, AggreGateException;
}
