export function formatEuro(budget: string | null): string {
	if (!budget || isNaN(Number(budget))) return "Not specified";
	
	return new Intl.NumberFormat("de-DE", {
		style: "currency",
		currency: "EUR",
		minimumFractionDigits: 0,
		maximumFractionDigits: 0,
	}).format(Number(budget));
}

// Version control for the application
const VERSION = "[test-04.08.2025]";  // Set the version of the application
const EXPIRY_DATE = "2025-08-18T23:59:59.999Z"; // Set the expiry date for the test version
const IS_TEST = true; // Set to true for test versions, false for production

export const getVersion = () => VERSION;
export const isExpired = () => IS_TEST && new Date() > new Date(EXPIRY_DATE);
export const showVersion = () => IS_TEST && !isExpired();