package net.gini.pay.ginipaybusiness.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collect
import net.gini.android.models.Document
import net.gini.pay.ginipaybusiness.GiniBusiness
import net.gini.pay.ginipaybusiness.R
import net.gini.pay.ginipaybusiness.databinding.GpbFragmentReviewBinding
import net.gini.pay.ginipaybusiness.review.model.PaymentDetails
import net.gini.pay.ginipaybusiness.review.model.ResultWrapper
import net.gini.pay.ginipaybusiness.review.pager.DocumentPageAdapter
import net.gini.pay.ginipaybusiness.util.autoCleared
import net.gini.pay.ginipaybusiness.util.setTextIfDifferent

data class ReviewConfiguration(
    val documentOrientation: Orientation = Orientation.Horizontal
)

enum class Orientation { Horizontal, Vertical }

class ReviewFragment(
    private val giniBusiness: GiniBusiness,
    private val configuration: ReviewConfiguration = ReviewConfiguration()
) : Fragment() {

    private val viewModel: ReviewViewModel by viewModels { getReviewViewModelFactory(giniBusiness) }
    private var binding: GpbFragmentReviewBinding by autoCleared()
    private var documentPageAdapter: DocumentPageAdapter by autoCleared()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        documentPageAdapter = DocumentPageAdapter(giniBusiness, configuration)
        binding = GpbFragmentReviewBinding.inflate(inflater).apply {
            configureOrientation()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            setStateListeners()
            setInputListeners()
            payment.setOnClickListener {
                viewModel.validatePaymentDetails()
                Toast.makeText(requireContext(), viewModel.paymentDetails.value.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun GpbFragmentReviewBinding.setStateListeners() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.giniBusiness.documentFlow.collect { handleDocumentResult(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.giniBusiness.paymentFlow.collect { handlePaymentResult(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.paymentDetails.collect { setPaymentDetails(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.paymentValidation.collect { handleValidationResult(it) }
        }
    }

    private fun GpbFragmentReviewBinding.handleDocumentResult(documentResult: ResultWrapper<Document>) {
        when (documentResult) {
            is ResultWrapper.Loading -> {
                // TODO
            }
            is ResultWrapper.Success -> {
                documentPageAdapter.submitList(viewModel.getPages(documentResult.value))
            }
            is ResultWrapper.Error -> {
                // TODO
            }
        }
    }

    private fun GpbFragmentReviewBinding.handlePaymentResult(paymentResult: ResultWrapper<PaymentDetails>) {
        binding.loading.isVisible = paymentResult is ResultWrapper.Loading
        when (paymentResult) {
            is ResultWrapper.Loading -> {
                // TODO
            }
            is ResultWrapper.Success -> {
                // TODO?
            }
            is ResultWrapper.Error -> {
                // TODO
            }
        }
    }

    private fun GpbFragmentReviewBinding.configureOrientation() {
        when (configuration.documentOrientation) {
            Orientation.Horizontal -> {
                pager.isVisible = true
                indicator.isVisible = true
                pager.adapter = documentPageAdapter
                TabLayoutMediator(indicator, pager) { tab, _ -> tab.view.isClickable = false }.attach()
            }
            Orientation.Vertical -> {
                list.isVisible = true
                list.layoutManager = LinearLayoutManager(requireContext())
                list.adapter = documentPageAdapter
            }
        }
    }

    private fun GpbFragmentReviewBinding.setPaymentDetails(paymentDetails: PaymentDetails) {
        recipient.setTextIfDifferent(paymentDetails.recipient)
        iban.setTextIfDifferent(paymentDetails.iban)
        amount.setTextIfDifferent(paymentDetails.amount)
        purpose.setTextIfDifferent(paymentDetails.purpose)
    }

    private fun GpbFragmentReviewBinding.setInputListeners() {
        recipient.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setRecipient(text.toString()) })
        iban.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setIban(text.toString()) })
        amount.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setAmount(text.toString()) })
        purpose.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setPurpose(text.toString()) })
    }

    private fun GpbFragmentReviewBinding.handleValidationResult(messages: List<ValidationMessage>) {
        recipientLayout.error = ""
        ibanLayout.error = ""
        amountLayout.error = ""
        purposeLayout.error = ""

        TransitionManager.beginDelayedTransition(root)
        messages.forEach { message ->
            with(getField(message.field)) {
                if (error.isNullOrEmpty()) {
                    isErrorEnabled = true
                    error = getString(
                        when (message) {
                            is ValidationMessage.Empty -> R.string.gpb_error_input_empty
                            ValidationMessage.InvalidIban -> R.string.gpb_error_input_invalid_iban
                            ValidationMessage.InvalidCurrency -> R.string.gpb_error_input_invalid_Currency
                            ValidationMessage.NoCurrency -> R.string.gpb_error_input_no_currency
                            ValidationMessage.AmountFormat -> R.string.gpb_error_input_amount_format
                        }
                    )
                }
            }
        }
        if (recipientLayout.error.isNullOrEmpty()) recipientLayout.isErrorEnabled = false
        if (ibanLayout.error.isNullOrEmpty()) ibanLayout.isErrorEnabled = false
        if (amountLayout.error.isNullOrEmpty()) amountLayout.isErrorEnabled = false
        if (purposeLayout.error.isNullOrEmpty()) purposeLayout.isErrorEnabled = false
    }

    private fun GpbFragmentReviewBinding.getField(field: PaymentField) = when (field) {
        PaymentField.Recipient -> recipientLayout
        PaymentField.Iban -> ibanLayout
        PaymentField.Amount -> amountLayout
        PaymentField.Purpose -> purposeLayout
    }
}
