import "./style.css";
import {useState} from "react";
import {
	CalendarClock,
	HandCoins,
	CodeXml,
	SlidersHorizontal,
	Factory,
	Eye,
	X,
	MenuIcon,
} from "lucide-react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import {RangeSlider} from 'rsuite';
import 'rsuite/dist/rsuite-no-reset.min.css';


type Props = {
	filters: {
		industries: string[];
		status: string[];
		source: string[];
		budget: { min: number; max: number };
		deadLine: { from: string; to: string };
	};
	onFilterChange: (filters: Props["filters"]) => void;
	industries: string[];
};

const filterOption = {
	status: ["Forthcoming", "Open", "Closed"],
};

const Sidebar = ({filters, onFilterChange, industries}: Props) => {
	const [openDropdowns, setOpenDropdowns] = useState<Record<string, boolean>>({});
	const [industrySearchTerm, setIndustrySearchTerm] = useState("");
	const [isMobileOpen, setMobileOpen] = useState(false);
	
	const toggle = (key: string) => {
		setOpenDropdowns((prev) => ({
			...prev,
			[key]: !prev[key],
		}));
	};
	
	const toggleMobile = () => setMobileOpen((prev) => !prev);
	
	const handleCheckboxChange = (
		group: keyof Props["filters"],
		value: string
	) => {
		if (group === "deadLine") return;
		
		const groupValues = filters[group] as string[];
		const updateValues = groupValues.includes(value)
			? groupValues.filter((v) => v !== value)
			: [...groupValues, value];
		
		onFilterChange({...filters, [group]: updateValues});
	};
	
	// Razpored industrij po abecedi
	const sortedIndustries = [...industries].sort((a, b) =>
		a.localeCompare(b, undefined, {sensitivity: "base"})
	);
	
	// Filtriranje industrij glede na iskalni niz
	const filteredIndustries = sortedIndustries.filter((industry) =>
		industry.toLowerCase().includes(industrySearchTerm.toLowerCase())
	);
	
	const handleBudgetChange = (values: number[]) => {
		onFilterChange({...filters, budget: {min: values[0], max: values[1]}})
	};
	
	return (
		<>
			{ /*Filtri na telefonu*/ }
			<button className="filter-toggle-btn" onClick={toggleMobile}>
				<MenuIcon size={18} />
			</button>
			
			{ isMobileOpen && <div className="sidebar-overlay" onClick={toggleMobile}></div> }
			
			<aside className={`sidebar ${isMobileOpen ? "open" : ""}`}>
				<button className="close-btn" onClick={toggleMobile}>
					<X size={20} />
				</button>
				
				
				<div className="sidebar-header">
					<h2>
						<SlidersHorizontal/> Filters
					</h2>
				</div>
				
				{/* Industries dinamicni filter */}
				<div className="filter-group" key="industries">
					<div className="dropdown-wrapper">
						<div className="dropdown-header" onClick={() => toggle("industries")}>
							<Factory className="filter-icon" size={16}/> Industries{" "}
						</div>
						
						{openDropdowns["industries"] && (
							<div className="dropdown-content">
								<input
									type="text"
									placeholder="Search industries..."
									className="industry-dropdown-search"
									value={industrySearchTerm}
									onChange={(e) => setIndustrySearchTerm(e.target.value)}
								/>
								{filteredIndustries.map((industry) => (
									<label key={industry}>
										<input
											type="checkbox"
											checked={filters.industries.includes(industry)}
											onChange={() => handleCheckboxChange("industries", industry)}
										/>
										{industry}
									</label>
								))}
							</div>
						)}
					</div>
				</div>
				
				{Object.entries(filterOption).map(([key, values]) => (
					<div className="filter-group" key={key}>
						<div className="dropdown-wrapper">
							<div className="dropdown-header" onClick={() => toggle(key)}>
								{key === "status" && (
									<>
										<Eye size={16} className="filter-icon"/> Status
									</>
								)}
								{key === "source" && (
									<>
										<CodeXml className="filter-icon" size={16}/> Source
									</>
								)}
							</div>
							
							{openDropdowns[key] && (
								<div className="dropdown-content">
									{values.map((value) => {
										const getStatusClass = (status: string) => {
											switch (status.toLowerCase()) {
												case "closed":
													return "status-closed";
												case "open":
													return "status-open";
												case "forthcoming":
													return "status-forthcoming";
												default:
													return "";
											}
										};
										
										return (
											<label key={value} className="checkbox-label">
												<input
													type="checkbox"
													checked={(filters[key as keyof typeof filterOption] as string[]).includes(value)}
													onChange={() =>
														handleCheckboxChange(key as keyof Props["filters"], value)
													}
												/>
												<span className={key === "status" ? getStatusClass(value) : ""}>
							{value}
						</span>
											</label>
										);
									})}
								</div>
							)}
						</div>
					</div>
				))}
				
				{/* Budget slider */}
				<div className="filter-group">
					<div className="slider-container">
						<div className="slider-header">
							<div className="slider-title">
								<HandCoins/>
								<b>Budget</b>
							</div>
						</div>
						
						<div className="slider-wrapper">
							<RangeSlider
								min={0}
								max={100000000}
								step={10000}
								value={[filters.budget.min, filters.budget.max]}
								onChange={handleBudgetChange}
								className="budget-slider"
							/>
							
							<div className="slider-ticks">
								<div className="tick"></div>
								<div className="tick"></div>
								<div className="tick"></div>
								<div className="tick"></div>
								<div className="tick"></div>
								<div className="tick"></div>
								<div className="tick"></div>
							</div>
							
							<div className="slider-labels">
								<span>0€</span>
								<span>50M€</span>
								<span>100M€</span>
							</div>
							
							<div className="budget-display">
								{filters.budget.min.toLocaleString()}€ <br/> - <br/> {filters.budget.max.toLocaleString()}€
							</div>
						</div>
					</div>
				</div>
				
				{/* Datum obravnava za filter */}
				<div className="filter-group">
					<div className="slider-container">
						<label className="slider-title">
							{" "}
							<CalendarClock size={20}/> <b>Deadline</b>
						</label>
						<br/>
						
						{/* DatePicker za izbiro datuma */}
						<label className="slider-title">
							From:
						</label>
						<DatePicker
							selected={
								filters.deadLine.from
									? new Date(filters.deadLine.from.split(".").reverse().join("-"))
									: null
							}
							onChange={(date: Date | null) => {
								if (date) {
									const formatted = `${date.getDate().toString().padStart(2, '0')}.${(
										date.getMonth() + 1
									).toString().padStart(2, '0')}.${date.getFullYear()}`;
									onFilterChange({...filters, deadLine: { ...filters.deadLine, from: formatted }});
								}
							}}
							dateFormat="dd.MM.yyyy"
							placeholderText="From date"
							className="slider-title"
						/>

						<br/>

						<label className="slider-title">
							To:
						</label>
						<DatePicker
							selected={
								filters.deadLine.to
									? new Date(filters.deadLine.to.split(".").reverse().join("-"))
									: null
							}
							onChange={(date: Date | null) => {
								if (date) {
									const formatted = `${date.getDate().toString().padStart(2, '0')}.${(
										date.getMonth() + 1
									).toString().padStart(2, '0')}.${date.getFullYear()}`;
									onFilterChange({...filters, deadLine: { ...filters.deadLine, to: formatted }});
								}
							}}
							dateFormat="dd.MM.yyyy"
							placeholderText="To date"
							className="slider-title"
						/>
					</div>
				</div>
				
				
				
				{/* Reset filters gumb */}
				<button
					className="reset-button"
					onClick={() =>
						onFilterChange({
							industries: [],
							status: [],
							source: [],
							budget: {min: 0, max: 100000000},
							deadLine: { from: "", to: "" },
						})
					}
				>
					Reset Filters
				</button>
			</aside>
		</>
	);
};

export default Sidebar;