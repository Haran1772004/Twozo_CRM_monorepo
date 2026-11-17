'use strict';

 const getInputFields = require('../fields/update_company_fields');

const updateCompany = async (z, bundle) => {
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
        event: 4, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'update_company',
  noun: 'Company',
  display: {
    label: 'Update Company',
    description: 'Update the existing company.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: updateCompany,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
