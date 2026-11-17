'use strict';

 const getInputFields = require('../fields/add_follower_company_fields');

const addFollower = async (z, bundle) => {
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/action/mgmt',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
        type: 'mapFollower',
        event: 16, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'add_follower_to_company',
  noun: 'Add Followers to Company',
  display: {
    label: 'Add Followers to Company',
    description: 'Add followers to the company',
  },
  operation: {
    inputFields: [
        function (z, bundle) {
          return getInputFields(z, bundle);
        }
      ],
    perform: addFollower,
    sample: {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      company: '1',
    }
  }
};
