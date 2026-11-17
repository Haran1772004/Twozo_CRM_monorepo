'use strict';

 const getInputFields = require('../fields/update_activity_fields');

const updateActivity = async (z, bundle) => {
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/action/mgmt',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
        type: 'update',
        event: 10, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'update_activity',
  noun: 'Activity',
  display: {
    label: 'Update Activity',
    description: 'Update the existing activity.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: updateActivity,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
