const {
  config: authentication,
  befores = [],
  afters = [],
} = require('./authentication');

const contactAdded = require('./triggers/new_contact');
const contactUpdated = require('./triggers/update_contact');
const companyAdded = require('./triggers/new_company');
const companyUpdated = require('./triggers/update_company');
const dealAdded = require('./triggers/new_deal');
const dealUpdated = require('./triggers/update_deal');
const dealStageUpdated = require('./triggers/update_deal_stage');
const productAdded = require('./triggers/new_product');
const productUpdated = require('./triggers/update_product');
const activityAdded = require('./triggers/new_activity');
const activityUpdated = require('./triggers/update_activity');


const createContact = require('./actions/create_contact');
const updateContact = require('./actions/update_contact');
const createDeal = require('./actions/create_deal');
const updateDeal = require('./actions/update_deal');
const createCompany = require('./actions/create_company');
const updateCompany = require('./actions/update_company');
const createProduct = require('./actions/create_product');
const updateProduct = require('./actions/update_product');
const createActivity = require('./actions/create_activity');
const updateActivity = require('./actions/update_activity');
const addProductToDeal = require('./actions/add_product_to_deal');
const addNoteToCompany = require('./actions/add_note_to_company');
const addNoteToContact = require('./actions/add_note_to_contact');
const addNoteToDeal = require('./actions/add_note_to_deal');
const addTagToCompany = require('./actions/add_tag_to_company');
const addTagToContact = require('./actions/add_tag_to_contact');
const addTagToDeal = require('./actions/add_tag_to_deal');
const addFollowerToCompany = require('./actions/add_follower_to_company');
const addFollowerToContact = require('./actions/add_follower_to_contact');
const addFollowerToDeal = require('./actions/add_follower_to_deal');


const companyList = require('./resources/company');
const contactList = require('./resources/contact');
const dealList = require('./resources/deal');
const productList = require('./resources/product');
const activityList = require('./resources/activity');
const activitTypeList = require('./resources/activity_type')
const userList = require('./resources/user');
const tagList = require('./resources/tag');
const territoryList = require('./resources/territory');
const lifecycleStageList = require('./resources/lifecycle_stage');
const lifesycleStatusList = require('./resources/lifecycle_status');
const contactLostReasonList = require('./resources/contact_lost_reason');
const categoryList = require('./resources/category');
const timezoneList = require('./resources/timezone');
const contactSourceList = require('./resources/contact_source');
const dealTypeList = require('./resources/deal_type');
const dealSourceList = require('./resources/deal_source');
const pipelineList = require('./resources/pipeline');
const pipelineStageList = require('./resources/pipeline_stage');
const wonReasonList = require('./resources/won_reason');
const lostReasonList = require('./resources/lost_reason');
const businessTypeList = require('./resources/business_type');
const industryTypeList = require('./resources/industry_type');
const orgStatusList = require('./resources/org_status');


module.exports = {
  version: require('./package.json').version,
  platformVersion: require('zapier-platform-core').version,

  authentication,

  beforeRequest: [...befores],

  afterResponse: [...afters],

  // If you want your trigger to show up, you better include it here!
  triggers: {
    [contactAdded.key]: contactAdded,
    [contactUpdated.key]: contactUpdated,
    [companyAdded.key]: companyAdded,
    [companyUpdated.key]: companyUpdated,
    [dealAdded.key]: dealAdded,
    [dealUpdated.key]: dealUpdated,
    [dealStageUpdated.key]: dealStageUpdated,
    [productAdded.key]: productAdded,
    [productUpdated.key]: productUpdated,
    [activityAdded.key]: activityAdded,
    [activityUpdated.key]: activityUpdated
  },

  // If you want your searches to show up, you better include it here!
  searches: {},

  // If you want your creates to show up, you better include it here!
  creates: {   
    [createContact.key]: createContact,
    [updateContact.key]: updateContact,
    [createCompany.key]: createCompany,
    [updateCompany.key]: updateCompany,
    [createDeal.key]: createDeal,
    [updateDeal.key]: updateDeal,
    [createProduct.key]: createProduct,
    [updateProduct.key]: updateProduct,
    [createActivity.key]: createActivity,
    [updateActivity.key]: updateActivity,
    [addProductToDeal.key]: addProductToDeal,
    [addNoteToCompany.key]: addNoteToCompany,
    [addNoteToContact.key]: addNoteToContact,
    [addNoteToDeal.key]: addNoteToDeal,
    [addTagToCompany.key]: addTagToCompany,
    [addTagToContact.key]: addTagToContact,
    [addTagToDeal.key]: addTagToDeal,
    [addFollowerToCompany.key]: addFollowerToCompany,
    [addFollowerToContact.key]: addFollowerToContact,
    [addFollowerToDeal.key]: addFollowerToDeal,
  },

  resources: {
    [companyList.key]: companyList,
    [contactList.key]: contactList,
    [dealList.key]: dealList,
    [activityList.key]: activityList,
    [activitTypeList.key]: activitTypeList,
    [productList.key]: productList,
    [userList.key]: userList,
    [tagList.key]: tagList,
    [territoryList.key]: territoryList,
    [lifecycleStageList.key]: lifecycleStageList,
    [lifesycleStatusList.key]: lifesycleStatusList,
    [contactLostReasonList.key]: contactLostReasonList,
    [categoryList.key]: categoryList,
    [timezoneList.key]: timezoneList,
    [contactSourceList.key]: contactSourceList,
    [dealTypeList.key]: dealTypeList,
    [dealSourceList.key]: dealSourceList,
    [pipelineList.key]: pipelineList,
    [pipelineStageList.key]: pipelineStageList,
    [wonReasonList.key]: wonReasonList,
    [lostReasonList.key]: lostReasonList,
    [businessTypeList.key]: businessTypeList,
    [industryTypeList.key]: industryTypeList,
    [orgStatusList.key]: orgStatusList,
  },
};
