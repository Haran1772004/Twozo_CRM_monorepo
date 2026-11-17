'use strict';

 const getInputFields = require('../fields/update_contact_fields');

const updateContact = async (z, bundle) => {
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
        event: 2, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'update_contact',
  noun: 'Contact',
  display: {
    label: 'Update Contact',
    description: 'Update the existing contact.',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: updateContact,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
