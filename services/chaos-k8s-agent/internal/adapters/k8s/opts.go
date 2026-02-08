package k8s

import metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

func metav1GetOpts() metav1.GetOptions       { return metav1.GetOptions{} }
func metav1UpdateOpts() metav1.UpdateOptions { return metav1.UpdateOptions{} }
func metav1DeleteOpts() metav1.DeleteOptions { return metav1.DeleteOptions{} }
func metav1ListOpts(selector string) metav1.ListOptions {
	return metav1.ListOptions{LabelSelector: selector}
}
