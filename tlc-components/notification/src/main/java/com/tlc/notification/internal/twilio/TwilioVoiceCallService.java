package com.tlc.notification.internal.twilio;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.internal.twilio.reader.LocalNumberReader;
import com.tlc.notification.internal.twilio.reader.MobileNumberReader;
import com.tlc.notification.internal.twilio.reader.NumberReader;
import com.tlc.notification.internal.twilio.reader.TollFreeNumberReader;
import com.tlc.notification.internal.twilio.reader.VoipNumberReader;
import com.tlc.notification.internal.twilio.reader.model.AvailableResourcesData;
import com.tlc.notification.internal.twilio.token.TwilioTokenGenerator;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.call.AvailableCountries;
import com.tlc.notification.model.call.AvailableCountry;
import com.tlc.notification.model.call.AvailableLocalities;
import com.tlc.notification.model.call.AvailableLocality;
import com.tlc.notification.model.call.AvailableNumber;
import com.tlc.notification.model.call.AvailableNumbers;
import com.tlc.notification.model.call.AvailableState;
import com.tlc.notification.model.call.AvailableStates;
import com.tlc.notification.model.call.CallRecord;
import com.tlc.notification.model.call.CallRoutingDetails;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.model.call.CallerToken;
import com.tlc.notification.model.call.MobileNumber;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.model.call.NumberType;
import com.tlc.notification.model.call.PageDetails;
import com.tlc.notification.model.call.Recipient;
import com.tlc.notification.model.call.VoiceCall;
import com.tlc.notification.service.provider.VoiceCallServiceProvider;
import com.twilio.base.Page;
import com.twilio.base.ResourceSet;
import com.twilio.http.HttpMethod;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.Domains;
import com.twilio.rest.api.v2010.account.Application;
import com.twilio.rest.api.v2010.account.AvailablePhoneNumberCountry;
import com.twilio.rest.api.v2010.account.AvailablePhoneNumberCountryReader;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.IncomingPhoneNumber;
import com.twilio.rest.api.v2010.account.ValidationRequest;
import com.twilio.rest.api.v2010.account.call.Recording;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Client;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Dial.Record;
import com.twilio.twiml.voice.Hangup;
import com.twilio.twiml.voice.Number;
import com.twilio.twiml.voice.Parameter;
import com.twilio.twiml.voice.Pause;
import com.twilio.twiml.voice.Say;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * <p>
 * Provides the twilio service for voice calls.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TwilioVoiceCallService extends TwilioService implements VoiceCallServiceProvider {

    private static TwilioVoiceCallService voiceCallService;
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static final String QUESTION_MARK = "?";
    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioVoiceCallService.class);

    private final NumberReader localNumberReader;
    private final NumberReader mobileNumberReader;
    private final NumberReader tollFreeNumberReader;
    private final NumberReader voipNumberReader;
    private final TwilioTokenGenerator tokenGenerator;

    private final String twimlAppSid;
    private final String recordUrlDomain;
    private final String recordExtension;
    private final int pauseTimeInSeconds;

    private Application application;

    private TwilioVoiceCallService(final Configurations configurations) {
        super(configurations);
        final Configurations voiceCallConfigs = configurations.getNestedConfigs(NotificationKeys.CALL.getKey());

        localNumberReader = new LocalNumberReader(twilioRestClient);
        mobileNumberReader = new MobileNumberReader(twilioRestClient);
        tollFreeNumberReader = new TollFreeNumberReader(twilioRestClient);
        voipNumberReader = new VoipNumberReader(twilioRestClient);
        tokenGenerator = new TwilioTokenGenerator(accountServiceId, voiceCallConfigs);
        twimlAppSid = voiceCallConfigs.getConfig(NotificationKeys.TWIML_APP_SERVICE_ID.getKey());
        recordUrlDomain = voiceCallConfigs.getConfig(NotificationKeys.RECORD_URL_DOMAIN.getKey());
        recordExtension = voiceCallConfigs.getConfig(NotificationKeys.RECORD_EXTENSION.getKey());
        pauseTimeInSeconds = voiceCallConfigs.getInt(NotificationKeys.PAUSE_TIME_IN_SECONDS.getKey());

        fetchApplication();
    }

    /**
     * <p>
     * Provides the instance of {@link TwilioVoiceCallService}.
     * </p>
     *
     * @param configurations properties for initialize the {@link TwilioVoiceCallService} object
     * @return {@link TwilioVoiceCallService} object
     */
    public static TwilioVoiceCallService getInstance(final Configurations configurations) {
        final ReentrantReadWriteLock.WriteLock writeLock = LOCK.writeLock();

        writeLock.lock();

        try {
            if (Objects.isNull(voiceCallService)) {
                voiceCallService = new TwilioVoiceCallService(configurations);
            }
        } finally {
            writeLock.unlock();
        }

        return voiceCallService;
    }

    /**
     * {@inheritDoc}
     *
     * @param numberCriteria Represents the criteria to fetch countries.
     * @return {@link AvailableCountries}.
     */
    @Override
    public AvailableCountries fetchAvailableCountries(final NumberCriteria numberCriteria) {
        final AvailableCountries countries = new AvailableCountries();
        final AvailablePhoneNumberCountryReader availablePhoneNumberCountryReader = new AvailablePhoneNumberCountryReader();
        final Counter totalCountriesCounter = new Counter();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final Integer pageLimit = pageDetails.pageLimit();
        final ResourceSet<AvailablePhoneNumberCountry> availableCountries;

        try {
            availableCountries = availablePhoneNumberCountryReader.read(twilioRestClient);
        } catch (Throwable throwable) {
            LOGGER.warn(throwable.getMessage(), throwable);
            return AvailableCountries.empty();
        }

        final Consumer<AvailablePhoneNumberCountry> countryDetailsLoader = availableCountry -> {
            final String country = availableCountry.getCountry();
            final String countryCode = availableCountry.getCountryCode();

            countries.add(new AvailableCountry(country, countryCode));
        };

        if (Objects.isNull(pageLimit)) {
            availableCountries.forEach(countryDetailsLoader);
        } else {
            final String pageUrl = pageDetails.pageUrl();

            availablePhoneNumberCountryReader.setPageSize(pageLimit);
            final Page<AvailablePhoneNumberCountry> availableCountryPage = Objects.isNull(pageUrl)
                    ? availablePhoneNumberCountryReader.firstPage(twilioRestClient)
                    : availablePhoneNumberCountryReader.getPage(pageUrl, twilioRestClient);

            final String nextPageUrl = availableCountryPage.getNextPageUrl(Domains.NUMBERS.toString());
            final NumberType numberType = pageDetails.numberType();

            countries.setNextPageDetails(Objects.isNull(nextPageUrl) ? null : new PageDetails(numberType, nextPageUrl, pageLimit));
            availableCountryPage.getRecords().forEach(countryDetailsLoader);
        }
        totalCountriesCounter.increment(availableCountries.getPageSize());
        countries.setTotalAvailableResources(totalCountriesCounter.getCount());

        return countries;
    }

    /**
     * {@inheritDoc}
     *
     * @param numberCriteria Represents the criteria to fetch states.
     * @return {@link AvailableStates}.
     */
    @Override
    public AvailableStates fetchAvailableStates(final NumberCriteria numberCriteria) {
        final AvailableStates availableStates = new AvailableStates();
        final Counter totalStatusCounter = new Counter();
        final Collection<NumberType> numberTypes = numberCriteria.getNumberTypes();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final NumberType fromNumberType = pageDetails.numberType();
        final Integer limit = pageDetails.pageLimit();
        final boolean isLimitNull = Objects.isNull(limit);
        int pageLimit = isLimitNull ? 0 : limit;

        numberTypeIteration:
        for (final NumberType numberType : numberCriteria.getNumberTypesFrom(fromNumberType)) {
            final Optional<NumberType> nextType = numberType.nextType();
            final NumberReader numberReader = getNumberReader(numberType);
            final AvailableResourcesData<AvailableState> availableResourcesData = numberReader.fetchAvailableStates(cloneNumberCriteria(numberCriteria));

            totalStatusCounter.increment(availableResourcesData.size());

            do {
                final AvailableResourcesData<AvailableState> availableStatesHolder = isLimitNull
                        ? availableResourcesData : numberReader.fetchAvailableStates(numberCriteria);
                final String nextPageUrl = availableStatesHolder.getNextPageUrl();
                final Collection<AvailableState> states = availableStatesHolder.getAll();

                pageLimit -= states.size();
                final PageDetails nextPageDetails = getNextPageDetails(pageLimit, nextPageUrl, nextType, numberType, numberTypes);

                availableStates.set(states);
                availableStates.setNextPageDetails(nextPageDetails);

                if (0 >= pageLimit) {
                    break numberTypeIteration;
                }
                numberCriteria.setPageDetails(nextPageDetails);
            } while (Objects.nonNull(availableStates.getNextPageDetails().pageUrl()));
        }
        availableStates.setTotalAvailableResources(totalStatusCounter.getCount());

        return availableStates;
    }

    /**
     * {@inheritDoc}
     *
     * @param numberCriteria Represents the criteria to fetch localities.
     * @return {@link AvailableLocalities}.
     */
    @Override
    public AvailableLocalities fetchAvailableLocalities(final NumberCriteria numberCriteria) {
        final AvailableLocalities availableLocalities = new AvailableLocalities();
        final Counter totalLocalityCounter = new Counter();
        final Collection<NumberType> numberTypes = numberCriteria.getNumberTypes();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final NumberType fromNumberType = pageDetails.numberType();
        final Integer limit = pageDetails.pageLimit();
        final boolean isLimitNull = Objects.isNull(limit);
        int pageLimit = isLimitNull ? 0 : limit;

        numberTypeIteration:
        for (final NumberType numberType : numberCriteria.getNumberTypesFrom(fromNumberType)) {
            final Optional<NumberType> nextType = numberType.nextType();
            final NumberReader numberReader = getNumberReader(numberType);
            final AvailableResourcesData<AvailableLocality> availableResourcesData = numberReader.fetchAvailableLocalities(cloneNumberCriteria(numberCriteria));

            totalLocalityCounter.increment(availableResourcesData.size());

            do {
                final AvailableResourcesData<AvailableLocality> availableLocalitiesHolder = isLimitNull
                        ? availableResourcesData : numberReader.fetchAvailableLocalities(numberCriteria);
                final String nextPageUrl = availableLocalitiesHolder.getNextPageUrl();
                final Collection<AvailableLocality> localities = availableLocalitiesHolder.getAll();

                pageLimit -= localities.size();
                final PageDetails nextPageDetails = getNextPageDetails(pageLimit, nextPageUrl, nextType, numberType, numberTypes);

                availableLocalities.set(localities);
                availableLocalities.setNextPageDetails(nextPageDetails);

                if (0 >= pageLimit) {
                    break numberTypeIteration;
                }
                numberCriteria.setPageDetails(nextPageDetails);
            } while (Objects.nonNull(availableLocalities.getNextPageDetails().pageUrl()));
        }
        availableLocalities.setTotalAvailableResources(totalLocalityCounter.getCount());

        return availableLocalities;
    }

    /**
     * {@inheritDoc}
     *
     * @param numberCriteria Represents the criteria to fetch numbers.
     * @return {@link AvailableNumbers}.
     */
    @Override
    public AvailableNumbers fetchAvailableNumbers(final NumberCriteria numberCriteria) {
        final AvailableNumbers availableNumbers = new AvailableNumbers();
        final Counter totalNumberCounter = new Counter();
        final Collection<NumberType> numberTypes = numberCriteria.getNumberTypes();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final NumberType fromNumberType = pageDetails.numberType();
        final Integer limit = pageDetails.pageLimit();
        final boolean isLimitNull = Objects.isNull(limit);
        int pageLimit = isLimitNull ? 0 : limit;

        for (final NumberType numberType : numberCriteria.getNumberTypesFrom(fromNumberType)) {
            final Optional<NumberType> nextType = numberType.nextType();
            final NumberReader numberReader = getNumberReader(numberType);
            final AvailableResourcesData<AvailableNumber> availableResourcesData = numberReader.fetchAvailableNumbers(cloneNumberCriteria(numberCriteria));

            totalNumberCounter.increment(availableResourcesData.size());

            do {
                final AvailableResourcesData<AvailableNumber> availableNumbersHolder = isLimitNull ? availableResourcesData
                        : numberReader.fetchAvailableNumbers(numberCriteria);
                final String nextPageUrl = availableNumbersHolder.getNextPageUrl();
                final Collection<AvailableNumber> numbers = availableNumbersHolder.getAll();

                pageLimit -= numbers.size();
                final PageDetails nextPageDetails = getNextPageDetails(pageLimit, nextPageUrl, nextType, numberType, numberTypes);

                availableNumbers.set(numbers);
                availableNumbers.setNextPageDetails(nextPageDetails);

                if (0 >= pageLimit) {
                    break;
                }
                numberCriteria.setPageDetails(nextPageDetails);
            } while (Objects.nonNull(availableNumbers.getNextPageDetails().pageUrl()));
        }
        availableNumbers.setTotalAvailableResources(totalNumberCounter.getCount());

        return availableNumbers;
    }

    /**
     * {@inheritDoc}
     *
     * @param mobileNumber Represents the number to be bought.
     */
    @Override
    public void buyMobileNumber(final MobileNumber mobileNumber) {
        final IncomingPhoneNumber phoneNumber = IncomingPhoneNumber.creator(new PhoneNumber(mobileNumber.getMobileNumberAsString()))
                .setFriendlyName(mobileNumber.getFriendlyName()).setVoiceApplicationSid(twimlAppSid).create(twilioRestClient);

        mobileNumber.setServiceId(phoneNumber.getSid());
    }

    /**
     * {@inheritDoc}
     *
     * @param mobileNumber Represents the number to be updated.
     */
    @Override
    public void updateMobileNumberName(final MobileNumber mobileNumber) {
        IncomingPhoneNumber.updater(mobileNumber.getServiceId()).setFriendlyName(mobileNumber.getFriendlyName())
                .update(twilioRestClient);
    }

    /**
     * {@inheritDoc}
     *
     * @param mobileNumber Represents the number to be deleted.
     */
    @Override
    public void deleteNumber(final MobileNumber mobileNumber) {
        IncomingPhoneNumber.deleter(mobileNumber.getServiceId()).delete(twilioRestClient);
    }

    /**
     * {@inheritDoc}
     *
     * @param identity Represents the identity of the token to be generated.
     * @return {@link CallerToken}.
     */
    @Override
    public CallerToken generateToken(final String identity) {
        return tokenGenerator.generateCallerToken(identity);
    }

    /**
     * {@inheritDoc}
     *
     * @param callRoutingDetails Holds the details of the call.
     * @return A string indicating the call routing status or session ID.
     */
    @Override
    public String routeCall(final CallRoutingDetails callRoutingDetails) {
        final VoiceResponse.Builder voiceResponseBuilder = new VoiceResponse.Builder();
        final Collection<Recipient> recipients = callRoutingDetails.getRecipients();

        if (Objects.isNull(recipients) || recipients.isEmpty()) {
            final Say.Language language = getTwilioLanguage(callRoutingDetails.getCallerLocale().getLanguage());

            voiceResponseBuilder.say(new Say.Builder("Sorry, this number is not recognized.").language(language).build())
                    .pause(new Pause.Builder().length(pauseTimeInSeconds).build())
                    .say(new Say.Builder("Your call will be disconnected.").language(language).build())
                    .hangup(new Hangup.Builder().build());
        } else {
            final MobileNumber caller = callRoutingDetails.getCaller();
            final String mobileNumber = caller.getMobileNumberAsString();
            final String callerName = caller.getFriendlyName();
            final String forwardedBy = callRoutingDetails.getForwardedBy();
            final Dial.Builder dialBuilder = new Dial.Builder().callerId(mobileNumber);
            final String receiverKey = NotificationKeys.RECEIVER.getKey();
            final String callerKey = NotificationKeys.CALLER.getKey();
            final String forwardedByKey = NotificationKeys.FORWARDED_BY.getKey();
            final Application application = fetchApplication();
            final HttpMethod statusCallbackMethod = Objects.isNull(application) ? null : application.getStatusCallbackMethod();
            final List<Number.Event> numberEvents = Arrays.stream(Number.Event.values()).toList();
            final List<Client.Event> clientEvents = Arrays.stream(Client.Event.values()).toList();
            final CallType callType = callRoutingDetails.getCallType();
            final String statusCallback = fetchStatusCallBackUrl(application, callRoutingDetails.getCallbackParamsAsString());
            Parameter callerParam = null;
            Parameter forwardedByParam = null;

            for (final Recipient recipient : recipients) {
                final String recipientIdentity = recipient.identity();

                switch (callType) {
                    case INCOMING -> {
                        if (Objects.isNull(callerParam)) {
                            callerParam = new Parameter.Builder().name(callerKey).value(Objects.isNull(callerName)
                                    ? mobileNumber : callerName).build();
                        }
                        final Parameter receiverParam = new Parameter.Builder().name(receiverKey).value(recipient.aliasName()).build();
                        final Client.Builder clientBuilder = new Client.Builder(recipientIdentity);

                        clientBuilder.parameter(callerParam).parameter(receiverParam).statusCallback(statusCallback)
                                .statusCallbackMethod(statusCallbackMethod).statusCallbackEvents(clientEvents);

                        if (Objects.nonNull(forwardedBy)) {
                            if (Objects.isNull(forwardedByParam)) {
                                forwardedByParam = new Parameter.Builder().name(forwardedByKey).value(forwardedBy).build();
                            }
                            clientBuilder.parameter(forwardedByParam);
                        }
                        dialBuilder.client(clientBuilder.build());
                    }
                    case OUTGOING -> {
                        dialBuilder.answerOnBridge(true);
                        dialBuilder.number(new Number.Builder(recipientIdentity).statusCallback(statusCallback)
                                .statusCallbackMethod(statusCallbackMethod).statusCallbackEvents(numberEvents).build());
                    }
                }
            }

            if (callRoutingDetails.isCallRecordingEnabled()) {
                dialBuilder.record(Record.RECORD_FROM_ANSWER);
            }
            voiceResponseBuilder.dial(dialBuilder.build());
        }
        return voiceResponseBuilder.build().toXml();
    }

    /**
     * {@inheritDoc}
     *
     * @param callRoutingDetails Holds the details of the call.
     */
    @Override
    public void transferCall(final CallRoutingDetails callRoutingDetails) {
        Call.updater(callRoutingDetails.getCallId()).setTwiml(routeCall(callRoutingDetails)).update(twilioRestClient);
    }

    /**
     * {@inheritDoc}
     *
     * @param callId         Holds the ID of the call.
     * @param needCallRecord Represents the needs of the call recording.
     */
    @Override
    public void recordCall(final String callId, final boolean needCallRecord) {
        final ResourceSet<Recording> recordings = Recording.reader(callId).read(twilioRestClient);
        final Recording.Status status;

        if (needCallRecord) {
            final boolean hasNext = recordings.iterator().hasNext();
            status = Recording.Status.IN_PROGRESS;

            if (!hasNext) {
                Recording.creator(callId).create(twilioRestClient);
            }
        } else {
            status = Recording.Status.PAUSED;
        }

        for (final Recording recording : recordings) {
            Recording.updater(callId, recording.getSid(), status).update(twilioRestClient);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param childCallId Represents the child call ID.
     * @return Parent call ID.
     */
    @Override
    public String getParentCallId(final String childCallId) {
        return fetchCall(childCallId).getParentCallSid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void makeCall(final VoiceCall voiceCall) {
        final String sender = voiceCall.getSender().getMobileNumberAsString();
        final String coDialer = voiceCall.getCoDialer().getMobileNumberAsString();
        final Call call = Call.creator(new PhoneNumber(coDialer), new PhoneNumber(sender), voiceCall.getRedirectUri())
                .setRecord(voiceCall.recordEnable()).setStatusCallback(voiceCall.getCallBackUri()).create(twilioRestClient);

        voiceCall.setServiceId(call.getSid());
        voiceCall.setStatus(call.getStatus().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String redirectCall(final MobileNumber receiverNumber) {
        final Dial dial = new Dial.Builder().number(receiverNumber.getMobileNumberAsString()).build();
        final VoiceResponse response = new VoiceResponse.Builder().say(new Say.Builder("").build()).dial(dial).build();

        return response.toXml();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VoiceCall fetchCallDetails(final String callId) {
        final VoiceCall voiceCall = new VoiceCall();
        final Call call = fetchCall(callId);
        final String price = call.getPrice();
        final ResourceSet<Recording> recordings;

        try {
            recordings = Recording.reader(callId).readAsync(twilioRestClient).get();
        } catch (InterruptedException | ExecutionException e) {
            throw ErrorCode.get(NotificationErrorCodes.COULD_NOT_FIND_CALL_RECORDINGS);
        }

        recordings.forEach(record -> {
            final CallRecord callRecord = new CallRecord();

            callRecord.setServiceId(record.getSid());
            callRecord.setUri(String.format("%s%s", recordUrlDomain, record.getUri().toString()
                    .replace(NotificationKeys.JSON.getKey(), recordExtension)));
            voiceCall.addCallRecord(callRecord);
        });
        voiceCall.setServiceId(callId);
        voiceCall.setActionTime(call.getStartTime().toInstant().toEpochMilli());
        voiceCall.setCompletedTime(call.getEndTime().toInstant().toEpochMilli());
        voiceCall.addRecipient(new MobileNumber(call.getTo()));
        voiceCall.setSender(new MobileNumber(call.getFrom()));
        voiceCall.setDuration(call.getDuration());
        voiceCall.setPriceUnit(call.getPriceUnit().getCurrencyCode());

        if (Objects.nonNull(price)) {
            voiceCall.setPrice(BigDecimal.valueOf(Double.parseDouble(price)));
        }

        return voiceCall;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String validateNumber(final MobileNumber number, final URI callBackUri) {
        try {
            final ValidationRequest validationRequest = ValidationRequest.creator(new PhoneNumber(number.getMobileNumberAsString()))
                    .setStatusCallback(callBackUri).createAsync(twilioRestClient).get();

            return validationRequest.getValidationCode();
        } catch (InterruptedException | ExecutionException exception) {
            throw ErrorCode.get(NotificationErrorCodes.COULD_NOT_VALIDATE_NUMBER, exception.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String fetchCallStatus(final String callSid) {
        return fetchCall(callSid).getStatus().name();
    }

    /**
     * <p>
     * Fetches the call details by using call sid.
     * </p>
     *
     * @param callSid call sid for fetch the call details
     * @return {@link Call} object
     */
    private Call fetchCall(final String callSid) {
        try {
            return Call.fetcher(callSid).fetchAsync(twilioRestClient).get();
        } catch (InterruptedException | ExecutionException e) {
            throw ErrorCode.get(NotificationErrorCodes.COULD_NOT_FIND_CALL_DETAILS);
        }
    }

    /**
     * <p>
     * Fetches the {@link NumberReader} for the given {@link NumberType}.
     * </p>
     *
     * @param numberType Represents the type of the number.
     * @return {@link NumberReader} of the {@link NumberType}.
     */
    private NumberReader getNumberReader(final NumberType numberType) {
        return switch (numberType) {
            case LOCAL -> localNumberReader;
            case MOBILE -> mobileNumberReader;
            case TOLL_FREE -> tollFreeNumberReader;
            case VOIP -> voipNumberReader;
        };
    }

    /**
     * <p>
     * Clones the {@link NumberCriteria} with partial details.
     * </p>
     *
     * @param numberCriteria Represents the criteria to fetch numbers.
     * @return {@link NumberCriteria}.
     */
    private NumberCriteria cloneNumberCriteria(final NumberCriteria numberCriteria) {
        final NumberCriteria criteria = new NumberCriteria();

        criteria.setLocationDetails(numberCriteria.getLocationDetails());
        criteria.setNeedVoiceEnabledNumber(numberCriteria.needVoiceEnabledNumber());
        criteria.setNeedSmsEnabledNumber(numberCriteria.needSmsEnabledNumber());
        return criteria;
    }

    /**
     * <p>
     * Fetches the TWIML {@link Application} of the current environment.
     * </p>
     *
     * @return TWIML {@link Application}.
     */
    private Application fetchApplication() {
        if (Objects.isNull(application)) {
            try {
                application = Application.fetcher(twimlAppSid).fetch(twilioRestClient);
            } catch (final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, throwable.getMessage());
            }
        }
        return application;
    }

    /**
     * <p>
     * Fetches the URL of the status callback from the {@link Application}.
     * </p>
     *
     * @param callBackParams Represents the parameters of the callback.
     * @return Status callback URL.
     */
    private String fetchStatusCallBackUrl(final Application application, final String callBackParams) {
        if (Objects.isNull(application)) {
            return null;
        }
        final StringBuilder statusCallbackBuilder = new StringBuilder(application.getStatusCallback().toString());

        statusCallbackBuilder.append(QUESTION_MARK).append(callBackParams);
        return statusCallbackBuilder.toString();
    }

    /**
     * <p>
     * Fetches the details of the next page.
     * </p>
     *
     * @param pageLimit   The maximum number of results per page.
     * @param nextPageUrl The URL for the next page of results.
     * @param nextType    The next type of phone number being paginated.
     * @param numberType  The type of phone number being paginated.
     * @param numberTypes Holds the collection of {@link NumberType}.
     * @return {@link PageDetails}.
     */
    private PageDetails getNextPageDetails(final int pageLimit, final String nextPageUrl, final Optional<NumberType> nextType,
                                           final NumberType numberType, final Collection<NumberType> numberTypes) {
        return Objects.isNull(nextPageUrl) ? nextType.filter(numberTypes::contains).map(nextNumberType ->
                new PageDetails(nextNumberType, pageLimit)).orElseGet(() -> new PageDetails(numberType, pageLimit))
                : new PageDetails(numberType, nextPageUrl, pageLimit);
    }

    /**
     * <p>
     * Fetches the {@link Say.Language} by using Locale language.
     * </p>
     *
     * @param language Represents the language of the locale.
     * @return {@link Say.Language}.
     */
    private Say.Language getTwilioLanguage(final String language) {
        for (final Say.Language twilioLanguage : Say.Language.values()) {
            if (Objects.equals(language, twilioLanguage.toString())) {
                return twilioLanguage;
            }
        }
        return Say.Language.EN_US;
    }

    /**
     * <p>
     * Stops the {@link TwilioRestClient} by assigning null value.
     * </p>
     */
    public void stop() {
        twilioRestClient = null;
    }
}
