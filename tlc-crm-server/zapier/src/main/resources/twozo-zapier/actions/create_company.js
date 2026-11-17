'use strict';

 const getInputFields = require('../fields/create_company_fields');

const createCompany = async (z, bundle) => {
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
      event: 3
    },
  });

  return response.data;
};

module.exports = {
  key: 'create_company',
  noun: 'Company',
  display: {
    label: 'Create Company',
    description: 'Creates a new company.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: createCompany,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
