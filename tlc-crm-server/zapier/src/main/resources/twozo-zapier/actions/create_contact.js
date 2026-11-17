'use strict';

 const getInputFields = require('../fields/create_contact_fields');

const createContact = async (z, bundle) => {
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
      event: 1
    },
  });

  return response.data;
};

module.exports = {
  key: 'create_contact',
  noun: 'Contact',
  display: {
    label: 'Create Contact',
    description: 'Creates a new contact.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: createContact,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
