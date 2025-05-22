import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ubicate.ubicate.R
import com.ubicate.ubicate.model.BusLocation

class BusDetailsDialogFragment(
    private val busLocation: BusLocation,  // Información del bus
    private val eta: String,  // Tiempo estimado de llegada
    private val distance: String // Distancia
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_bus_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias a los elementos del layout
        val busIdTextView: TextView = view.findViewById(R.id.busIdTextView)
        val etaTextView: TextView = view.findViewById(R.id.etaTextView)
        val distanceTextView: TextView = view.findViewById(R.id.distanceTextView)
        val statusTextView: TextView = view.findViewById(R.id.statusTextView)

        // Establecer los valores de la UI
        busIdTextView.text = busLocation.busId
        etaTextView.text = "Llega en $eta"
        distanceTextView.text = "$distance -"
        statusTextView.text = " En ruta"  // Aquí puedes poner el estado dinámicamente si lo tienes
    }
}
