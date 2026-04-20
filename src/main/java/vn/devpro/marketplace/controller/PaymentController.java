package vn.devpro.marketplace.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.devpro.marketplace.entity.Order;
import vn.devpro.marketplace.service.KeyDeliveryService;
import vn.devpro.marketplace.service.OrderService;
import vn.devpro.marketplace.service.VNPayService;

import java.util.Map;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController extends BaseController {

    private final OrderService orderService;
    private final VNPayService vnPayService;
    private final KeyDeliveryService keyDeliveryService;

    @GetMapping("/vnpay")
    public String redirectToVnpay(@RequestParam Integer orderId, HttpServletRequest request) {
        Order order = orderService.findById(orderId);
        String paymentUrl = vnPayService.buildPaymentUrl(
            orderId, order.getFinalAmount(), order.getVnpayTxnRef(), request);
        return "redirect:" + paymentUrl;
    }

    @GetMapping("/callback")
    public String handleCallback(@RequestParam Map<String, String> params, Model model) {
        if (!vnPayService.verifyCallback(params)) {
            model.addAttribute("success", false);
            model.addAttribute("errorMessage", "Chữ ký không hợp lệ");
            return "payment-result";
        }

        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        String transactionId = params.get("vnp_TransactionNo");

        Order order = orderService.findByTxnRef(txnRef);

        if ("00".equals(responseCode)) {
            if (order.getStatus() == Order.OrderStatus.pending) {
                orderService.markPaid(order, transactionId);
                keyDeliveryService.deliver(order);
            }
            order = orderService.findById(order.getId());
            model.addAttribute("success", true);
            model.addAttribute("order", order);
        } else {
            if (order.getStatus() == Order.OrderStatus.pending) {
                orderService.markCancelled(order);
            }
            model.addAttribute("success", false);
            model.addAttribute("errorCode", responseCode);
            model.addAttribute("errorMessage", "Thanh toán thất bại (mã lỗi: " + responseCode + ")");
        }
        return "payment-result";
    }
}
