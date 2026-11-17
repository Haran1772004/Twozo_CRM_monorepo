'use strict';

 const getInputFields = require('../fields/add_product_deal_fields');

const addProductToDeal = async (z, bundle) => {
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/action/mgmt',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
        type: 'addProductToDeal',
        event: 11, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'add_product_to_deal',
  noun: 'Add Product to Deal',
  display: {
    label: 'Add Product to Deal',
    description: 'Add product to the deal',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: addProductToDeal,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
