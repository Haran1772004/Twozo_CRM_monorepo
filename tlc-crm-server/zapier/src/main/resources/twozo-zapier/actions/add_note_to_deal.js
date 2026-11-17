'use strict';

 const getInputFields = require('../fields/add_note_deal_fields');

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
        event: 14, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'add_note_to_deal',
  noun: 'Add Note to Deal',
  display: {
    label: 'Add Note to Deal',
    description: 'Add note to the deal',
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
