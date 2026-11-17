'use strict';

const subscribeHook = async (z, bundle) => {
  // Step 1: Subscribe to the webhook (POST with zapId)
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/event/webhook',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
      type: 'subscribe',
      zapId: bundle.meta.zap.id, 
      hookUrl: bundle.targetUrl,  
      event: 5 
    },
  });
  return {
    subscriptionId: response.data.data.id
  }; 
};

const unsubscribeHook = async (z, bundle) => {
  // Step 2: Unsubscribe (POST with zapId)
  await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/event/webhook',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
      type: 'unsubscribe',
      id: bundle.subscribeData.subscriptionId,
      zapId: bundle.meta.zap.id, 
      event: 5
    },
  });
  return { status: 'success' };
};

const getDeal = async (z, bundle) => {
  // Step 3: Handle incoming webhook data (optional)
  return [bundle.cleanedRequest];
};

const sampleList = async () => {
    return [
      {
        id: 1,
        title: 'Cyrene Deal'
      },
      {
        id: 2,
        title: 'Reynard Deal'
      }
    ];
  };

module.exports = {
  key: 'new_deal',
  noun: 'Deal added',
  display: {
    label: 'New Deal Added',
    description: 'Triggers when a new deal is added.',
  },
  operation: {
    type: 'hook',
    performSubscribe: subscribeHook,
    performUnsubscribe: unsubscribeHook,
    perform: getDeal,
    performList: sampleList,
    sample: {title: 'Cyrene Deal' },
  },
};