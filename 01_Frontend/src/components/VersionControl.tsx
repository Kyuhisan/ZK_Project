import { isExpired, showVersion, getVersion } from './utils';

export const VersionBanner = () => {
	if (!showVersion()) return null;
	
	return (
		<div style={{
			padding: '8px',
			textAlign: 'center',
			backgroundColor: '#fffbeb',
			color: '#92400e',
			fontSize: '13px',
			fontWeight: 'bold',
			borderBottom: '1px solid #e5e7eb'
		}}>
			Application Version: {getVersion()}
		</div>
	);
};

export const ExpiredOverlay = () => {
	if (!isExpired()) return null;
	
	return (
		<div style={{
			position: 'fixed',
			inset: 0,
			backgroundColor: 'rgba(255,255,255,0.95)',
			display: 'flex',
			alignItems: 'center',
			justifyContent: 'center',
			zIndex: 9999
		}}>
			<div style={{
				padding: '32px',
				backgroundColor: 'white',
				border: '2px solid #fecaca',
				borderRadius: '12px',
				textAlign: 'center',
				maxWidth: '400px'
			}}>
				<h1 style={{ color: '#dc2626', marginBottom: '16px' }}>
					Test version expired!
				</h1>
				<p style={{ color: '#6b7280' }}>
					For more information, please contact the developers.
				</p>
			</div>
		</div>
	);
};