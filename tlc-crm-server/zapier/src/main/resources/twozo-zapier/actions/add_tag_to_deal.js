'use strict';

 const getInputFields = require('../fields/add_tag_deal_fields');

const addTag = async (z, bundle) => {
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/action/mgmt',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
        type: 'mapTag',
        event: 20, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'add_tag_to_deal',
  noun: 'Add Tags to Deal',
  display: {
    label: 'Add Tags to Deal',
    description: 'Add tags to the deal',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: addTag,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
