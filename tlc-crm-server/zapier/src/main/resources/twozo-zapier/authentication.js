'use strict';

const test = (z, bundle) => {
  return z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/user/authenticate',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json',
    },
    body: {},
  });
};

// Middleware to inject headers into every request
const includeAuthHeaders = (request, z, bundle) => {
  request.headers = request.headers || {};
  request.headers['Authorization'] = `Bearer ${bundle.authData.access_token}`;
  request.headers['X-EMAIL'] = bundle.authData.email;

  return request;
};

const handleBadResponses = (response, z, bundle) => {
  if (response.status === 401) {
    throw new z.errors.Error(
      'The credentials you supplied are incorrect',
      'Authentication Error',
      response.status
    );
  }
  return response;
};

module.exports = {
  config: {
    type: 'basic',
    
    // Define additional fields needed for authentication
    fields: [
      {
        key: 'username',
        label: 'Email',
        type: 'string',
        required: true,
        helpText: 'Enter your account email.',
      },
      {
        key: 'password',
        label: 'Password',
        type: 'password',
        required: true,
        helpText: 'Enter your account password.',
      }
      // Note: Basic auth automatically includes username/password fields
      // so we don't need to define them here
    ],

    // Define the headers that will be added to every request
    // requestTemplate: {
    //   headers: {
    //     'Authorization': 'Bearer {{bundle.authData.access_token}}',
    //     'X-EMAIL': '{{bundle.authData.email}}'
    //   }
    // },

    test: test,
    connectionLabel: '{{bundle.authData.username}}', // Using email as connection label
  },
  befores: [includeAuthHeaders],
  afters: [handleBadResponses],
};