const PAGE_SIZE = 50;

const contactList = async (z, bundle) => {
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
            name: 'contactList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((cont) => ({
    id: cont.id.toString(),
    name: cont.name
  }));
  };
  
  module.exports = {
    key: 'contact_list',
    noun: 'Contact List',
    list: {
        display: {
          label: 'List Contacts',
          description: 'Fetches a list of contacts for dropdown.',
          hidden: true
        },
        operation: {
          perform: contactList,
          canPaginate: true
        },
      },
  };
  