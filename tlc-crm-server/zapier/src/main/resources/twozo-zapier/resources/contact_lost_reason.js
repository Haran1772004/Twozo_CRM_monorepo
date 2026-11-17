const PAGE_SIZE = 50;

const contactLostReason = async (z, bundle) => {
    const response = await z.request({
        method: 'POST',
        url: 'https://crm.twozo.io/api/zap/dropdown',
        headers: {
          'Authorization': `Bearer ${bundle.authData.access_token}`,
          'X-EMAIL': bundle.authData.email,
          'Content-Type': 'application/json'
        },
        body: {
            page: bundle.meta.page,
            name: 'contactLostReasonList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((reason) => ({
    id: reason.id.toString(),
    name: reason.name
  }));
  };
  
  module.exports = {
    key: 'contactLostReason_list',
    noun: 'Contact Lost Reason List',
    list: {
        display: {
          label: 'List Contact Lost Reason',
          description: 'Fetches a list of contact lost reasons for dropdown.',
          hidden: true
        },
        operation: {
          perform: contactLostReason,
          canPaginate: true
        },
      },
  };
  