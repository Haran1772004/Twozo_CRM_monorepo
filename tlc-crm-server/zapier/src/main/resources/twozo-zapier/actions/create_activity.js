'use strict';

 const getInputFields = require('../fields/create_activity_fields');

const createActivity = async (z, bundle) => {
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/action/mgmt',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
      type: 'create',
      struct: bundle.inputData,
      event: 9
    },
  });

  return response.data;
};

module.exports = {
  key: 'create_activity',
  noun: 'Activity',
  display: {
    label: 'Create Activity',
    description: 'Creates a new activity.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: createActivity,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
