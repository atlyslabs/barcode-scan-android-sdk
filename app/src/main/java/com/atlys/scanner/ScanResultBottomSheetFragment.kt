package com.atlys.scanner

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atlys.barcode_scan.BarcodeResult
import com.atlys.scanner.databinding.FragmentScanResultBottomSheetBinding
import com.atlys.scanner.databinding.ItemBarcodeResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ScanResultBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentScanResultBottomSheetBinding? = null
    private val binding get() = _binding!!

    var barcodeResults: List<BarcodeResult> = emptyList()

    var onDismissListener: (() -> Unit)? = null

    override fun getTheme(): Int {
        return R.style.RoundedBottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanResultBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView using View Binding
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = BarcodeResultAdapter(barcodeResults)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

}

class BarcodeResultAdapter(private val items: List<BarcodeResult>) :
    RecyclerView.Adapter<BarcodeResultAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemBarcodeResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BarcodeResult) {
            binding.imageView.setImageBitmap(item.bitmap)
            binding.textView.text = item.rawValue
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBarcodeResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}