'use strict';

 const getInputFields = require('../fields/add_follower_deal_fields');

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
        event: 17, 
        struct: bundle.inputData
    },
  });

  return response.data;
};

module.exports = {
  key: 'add_follower_to_deal',
  noun: 'Add Followers to Deal',
  display: {
    label: 'Add Followers to Deal',
    description: 'Add followers to the deal',
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
