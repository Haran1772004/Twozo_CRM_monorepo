'use strict';

 const getInputFields = require('../fields/update_product_fields');

const updateProduct = async (z, bundle) => {
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
        event: 8, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'update_product',
  noun: 'Product',
  display: {
    label: 'Update Product',
    description: 'Update the existing product.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: updateProduct,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
