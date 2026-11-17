'use strict';

 const getInputFields = require('../fields/update_deal_fields');

const updateDeal = async (z, bundle) => {
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
        event: 6, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'update_deal',
  noun: 'Deal',
  display: {
    label: 'Update Deal',
    description: 'Update the existing deal.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: updateDeal,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
