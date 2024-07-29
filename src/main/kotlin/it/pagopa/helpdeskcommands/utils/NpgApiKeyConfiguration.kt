package it.pagopa.helpdeskcommands.utils

import arrow.core.Either
import arrow.core.left
import it.pagopa.helpdeskcommands.exceptions.NpgApiKeyConfigurationException
import java.util.*

class NpgApiKeyConfiguration(
    private val defaultApiKey: String?,
    private val methodsApiKeyMapping: Map<PaymentMethod, NpgPspApiKeysConfig>
) {

    init {
        require(defaultApiKey != null) {
            throw NpgApiKeyConfigurationException(
                "Invalid configuration detected! Default api key mapping cannot be null"
            )
        }
        require(methodsApiKeyMapping.isNotEmpty()) {
            throw NpgApiKeyConfigurationException(
                "Invalid configuration detected! Payment methods api key mapping cannot be null or empty"
            )
        }
    }

    data class Builder(var defaultApiKey: String? = null) {
        var methodsApiKeyMapping: MutableMap<PaymentMethod, NpgPspApiKeysConfig> = mutableMapOf()

        fun setDefaultApiKey(defaultApiKey: String) = apply { this.defaultApiKey = defaultApiKey }
        fun withMethodPspMapping(
            paymentMethod: PaymentMethod,
            npgPspApiKeysConfig: NpgPspApiKeysConfig
        ) = apply {
            if (methodsApiKeyMapping.containsKey(paymentMethod)) {
                throw NpgApiKeyConfigurationException(
                    "Api key mapping already registered for payment method: [$paymentMethod]"
                )
            }
            methodsApiKeyMapping.put(paymentMethod, npgPspApiKeysConfig)
        }
        fun build() = NpgApiKeyConfiguration(defaultApiKey, methodsApiKeyMapping)
    }

    /**
     * Alias for {@link NpgApiKeyConfiguration#getApiKeyForPaymentMethod(NpgClient.PaymentMethod,
     * String)}
     *
     * @param paymentMethod the payment method for which api keys will be searched for
     * @param pspId the searched api key psp id
     * @return either the found api key or an NpgApiKeyConfigurationException exception if no api
     *   key can be found
     */
    operator fun get(paymentMethod: PaymentMethod, pspId: String) =
        getApiKeyForPaymentMethod(paymentMethod, pspId)

    /**
     * Get the default NPG api key
     *
     * @return the default api key
     */
    fun getDefaultApiKey() = defaultApiKey

    /**
     * Get the api key associated to the input pspId for the given paymentMethod
     *
     * @param paymentMethod the payment method for which api keys will be searched for
     * @param pspId the searched api key psp id
     * @return either the found api key or an NpgApiKeyConfigurationException exception if no api
     *   key can be found
     */
    fun getApiKeyForPaymentMethod(
        paymentMethod: PaymentMethod,
        pspId: String
    ): Either<NpgApiKeyConfigurationException, String> {
        var result: Either<NpgApiKeyConfigurationException, String> =
            NpgApiKeyConfigurationException(
                    "Cannot retrieve api key configuration for payment method: [$paymentMethod]."
                )
                .left()
        val npgPspApiKeysConfig = methodsApiKeyMapping[paymentMethod]
        if (npgPspApiKeysConfig != null) {
            result =
                npgPspApiKeysConfig.get(pspId).mapLeft {
                    NpgApiKeyConfigurationException(
                        "Cannot retrieve api key for payment method: [$paymentMethod]. Cause: ${it.message}"
                    )
                }
        }
        return result
    }
}