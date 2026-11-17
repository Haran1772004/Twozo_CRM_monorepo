'use strict';

 const getInputFields = require('../fields/create_deal_fields');

const createDeal = async (z, bundle) => {
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
      event: 5
    },
  });

  return response.data;
};

module.exports = {
  key: 'create_deal',
  noun: 'Deal',
  display: {
    label: 'Create Deal',
    description: 'Creates a new deal.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: createDeal,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
