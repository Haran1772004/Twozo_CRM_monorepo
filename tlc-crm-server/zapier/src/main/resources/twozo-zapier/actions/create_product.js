'use strict';

 const getInputFields = require('../fields/create_product_fields');

const createProduct = async (z, bundle) => {
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
      event: 7
    },
  });

  return response.data;
};

module.exports = {
  key: 'create_product',
  noun: 'Product',
  display: {
    label: 'Create Product',
    description: 'Creates a new product.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: createProduct,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
