'use strict';

 const getInputFields = require('../fields/add_note_contact_fields');

const addNote = async (z, bundle) => {
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/action/mgmt',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
        type: 'mapNote',
        event: 12, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'add_note_to_contact',
  noun: 'Add Note to Contact',
  display: {
    label: 'Add Note to Contact',
    description: 'Add note to the contact',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: addNote,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
