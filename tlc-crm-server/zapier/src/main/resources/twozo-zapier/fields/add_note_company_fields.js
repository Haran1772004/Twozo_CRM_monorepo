'use strict';

const getInputFields = async (z, bundle) => {
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/action/mgmt',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json',
    },
    body: {
      type: 'fields',
      id: 13
    },
  });
  z.console.log('API Response:', response.data);

  const list = response.data?.data?.list;
  if (!Array.isArray(list)) {
    throw new z.errors.HaltedError('Input fields API did not return an array');
  }

  return list.map(field => {
    const fieldDef = {
      key: field.key,
      label: field.label,
      required: field.required || false,
      type: field.type || 'string'
    };

    if (field.dynamic) {
      fieldDef.dynamic = field.dynamic;
    }

    if (field.list) {
      fieldDef.list = field.list;
    }

    if (field.choices) {
      fieldDef.choices = field.choices
    }

    return fieldDef;
  });
};

// Required for Zapier validator to avoid crashing
getInputFields.toString = () => '[Function: getInputFields]';

module.exports = getInputFields;
